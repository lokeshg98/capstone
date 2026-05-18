package com.communitybot.ai.agent;

import com.communitybot.ai.config.AgentProperties;
import com.communitybot.ai.dto.AskCitation;
import com.communitybot.ai.dto.AskStep;
import com.communitybot.workspace.service.WorkspaceService;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultiAgentService {

    private static final String SYSTEM_FULL = """
            You are Community Bot for a Slack-style community workspace.
            You can call tools to search FAQ documents, recall the member's long-term memories, search channel messages,
            semantically search indexed messages, fetch or summarize threads, search the web when allowed,
            inspect moderation flags/bans/stats, and propose scheduled posts (which always require human confirmation in the UI).

            Prefer tools instead of guessing. When FAQ excerpts are returned, mention the document titles in your answer.
            If a tool says a feature is disabled or access was denied, explain that politely.
            Keep the final reply helpful and concise unless the user requests depth.
            """;

    private static final String SYSTEM_CHANNEL = """
            You are Community Bot answering a @bot mention inside a channel thread.
            Be brief. You may use FAQ search and long-term memory recall tools.
            Other tools will refuse in this mode — if the user needs them, tell them to open "Ask Bot" from the sidebar.
            """;

    private final ChatModelBridge       models;
    private final CommunityAgentTools   tools;
    private final RedisChatMemoryStore  chatMemoryStore;
    private final AgentProperties       agentProperties;
    private final LongTermMemoryService longTermMemoryService;
    private final WorkspaceService      workspaceService;
    private final AgentRateLimiter      agentRateLimiter;
    private final AgentMetrics          agentMetrics;

    @Component
    @RequiredArgsConstructor
    public static class ChatModelBridge {
        private final ChatModel           chatModel;
        private final StreamingChatModel streamingChatModel;

        public ChatModel chat() {
            return chatModel;
        }

        public StreamingChatModel streaming() {
            return streamingChatModel;
        }
    }

    public record AgentAnswer(
            String answer,
            int sourceChunks,
            List<AskCitation> citations,
            List<AskStep> steps
    ) {}

    private ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(agentProperties.getShortTermMemoryMaxMessages())
                .chatMemoryStore(chatMemoryStore)
                .build();
    }

    private CommunityAssistant buildAssistant(boolean constrainedChannel) {
        return AiServices.builder(CommunityAssistant.class)
                .chatModel(models.chat())
                .systemMessage(constrainedChannel ? SYSTEM_CHANNEL : SYSTEM_FULL)
                .tools(tools)
                .chatMemoryProvider(chatMemoryProvider())
                .maxSequentialToolsInvocations(constrainedChannel
                        ? agentProperties.getMaxSequentialToolsConstrained()
                        : agentProperties.getMaxSequentialTools())
                .beforeToolExecution(before -> {
                    AgentContext ctx = AgentContextHolder.get();
                    if (ctx != null) {
                        ctx.emit(AgentStreamEvent.toolStart(
                                before.request().name(),
                                trunc(before.request().arguments(), 900)));
                    }
                })
                .afterToolExecution(after -> {
                    agentMetrics.recordTool(after.request().name());
                    AgentContext ctx = AgentContextHolder.get();
                    if (ctx != null) {
                        ctx.emit(AgentStreamEvent.toolResult(
                                after.request().name(),
                                trunc(after.result(), 1200)));
                    }
                })
                .build();
    }

    private StreamingCommunityAssistant buildStreamingAssistant() {
        return AiServices.builder(StreamingCommunityAssistant.class)
                .streamingChatModel(models.streaming())
                .systemMessage(SYSTEM_FULL)
                .tools(tools)
                .chatMemoryProvider(chatMemoryProvider())
                .maxSequentialToolsInvocations(agentProperties.getMaxSequentialTools())
                .beforeToolExecution(before -> {
                    AgentContext ctx = AgentContextHolder.get();
                    if (ctx != null) {
                        ctx.emit(AgentStreamEvent.toolStart(
                                before.request().name(),
                                trunc(before.request().arguments(), 900)));
                    }
                })
                .afterToolExecution(after -> {
                    agentMetrics.recordTool(after.request().name());
                    AgentContext ctx = AgentContextHolder.get();
                    if (ctx != null) {
                        ctx.emit(AgentStreamEvent.toolResult(
                                after.request().name(),
                                trunc(after.result(), 1200)));
                    }
                })
                .build();
    }

    public AgentAnswer answerSync(UUID workspaceId, UUID userId, String conversationId,
                                  String question, boolean constrainedChannel) {
        agentRateLimiter.consume(workspaceId);
        workspaceService.requireMembership(workspaceId, userId);

        String memId = (conversationId == null || conversationId.isBlank())
                ? "default-" + userId
                : conversationId;

        AgentRunStateHolder.init();
        AgentContextHolder.set(constrainedChannel
                ? AgentContext.channelBot(workspaceId, userId)
                : AgentContext.sync(workspaceId, userId));

        try {
            String memories = longTermMemoryService.formatRecallForPrompt(workspaceId, userId, question);
            String composed = question;
            if (!memories.isBlank()) {
                composed = "Relevant long-term memories:\n" + memories + "\n\nUser:\n" + question;
            }

            CommunityAssistant assistant = buildAssistant(constrainedChannel);
            String answer = assistant.chat(memId, composed);

            AgentRunState st = AgentRunStateHolder.get();
            List<AskCitation> citations = List.of();
            List<AskStep> steps = List.of();
            int n = 0;
            if (st != null) {
                n = st.sourceChunkCount();
                citations = st.citations().stream()
                        .map(c -> new AskCitation(c.documentTitle(), c.chunkText(), c.chunkIndex()))
                        .toList();
                steps = st.steps().stream()
                        .map(s -> new AskStep(s.kind(), s.detail()))
                        .toList();
            }

            if (answer != null && !answer.isBlank()) {
                longTermMemoryService.rememberTurn(workspaceId, userId, question, answer);
            }
            agentMetrics.recordCompletion();
            return new AgentAnswer(answer == null ? "" : answer, n, citations, steps);
        } finally {
            AgentContextHolder.clear();
            AgentRunStateHolder.clear();
        }
    }

    public void streamAnswer(SseEmitter emitter, UUID workspaceId, UUID userId,
                             String conversationId, String question) {
        agentRateLimiter.consume(workspaceId);
        workspaceService.requireMembership(workspaceId, userId);

        String memId = (conversationId == null || conversationId.isBlank())
                ? "default-" + userId
                : conversationId;

        AgentRunStateHolder.init();

        java.util.function.Consumer<AgentStreamEvent> sink = ev -> {
            try {
                emitter.send(SseEmitter.event().name("agent").data(ev, MediaType.APPLICATION_JSON));
            } catch (Exception ex) {
                log.debug("SSE client disconnected: {}", ex.getMessage());
            }
        };

        AgentContextHolder.set(AgentContext.streaming(workspaceId, userId, sink));

        try {
            emitter.send(SseEmitter.event().name("agent").data(
                    AgentStreamEvent.thinking("Planning and calling tools as needed…"),
                    MediaType.APPLICATION_JSON));

            String memories = longTermMemoryService.formatRecallForPrompt(workspaceId, userId, question);
            String composed = question;
            if (!memories.isBlank()) {
                composed = "Relevant long-term memories:\n" + memories + "\n\nUser:\n" + question;
            }

            StreamingCommunityAssistant assistant = buildStreamingAssistant();
            StringBuilder tokenAcc = new StringBuilder();

            assistant.chat(memId, composed)
                    .onPartialResponse(chunk -> {
                        tokenAcc.append(chunk);
                        sink.accept(AgentStreamEvent.tokenDelta(chunk));
                    })
                    .onCompleteResponse(resp -> {
                        try {
                            String text = resp.aiMessage() != null ? resp.aiMessage().text() : tokenAcc.toString();
                            AgentRunState st = AgentRunStateHolder.get();
                            List<AgentStreamEvent.CitationPayload> cits =
                                    st != null ? st.citations() : List.of();
                            List<AgentStreamEvent.StepPayload> stps =
                                    st != null ? st.steps() : List.of();
                            var proposal = st != null ? st.proposal() : null;

                            if (text != null && !text.isBlank()) {
                                longTermMemoryService.rememberTurn(workspaceId, userId, question, text);
                            }
                            agentMetrics.recordCompletion();

                            sink.accept(AgentStreamEvent.done(new AgentStreamEvent.AgentAnswerPayload(
                                    text,
                                    cits.size(),
                                    cits,
                                    stps,
                                    proposal
                            )));
                            emitter.complete();
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        } finally {
                            AgentContextHolder.clear();
                            AgentRunStateHolder.clear();
                        }
                    })
                    .onError(err -> {
                        try {
                            emitter.completeWithError(err);
                        } finally {
                            AgentContextHolder.clear();
                            AgentRunStateHolder.clear();
                        }
                    })
                    .start();

        } catch (Exception e) {
            AgentContextHolder.clear();
            AgentRunStateHolder.clear();
            emitter.completeWithError(e);
        }
    }

    private static String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
