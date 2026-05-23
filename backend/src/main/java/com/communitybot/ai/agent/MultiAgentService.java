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

    private final ChatModelBridge       models;
    private final CommunityAgentTools   tools;
    private final RedisChatMemoryStore  chatMemoryStore;
    private final AgentProperties       agentProperties;
    private final LongTermMemoryService longTermMemoryService;
    private final WorkspaceService      workspaceService;
    private final AgentRateLimiter      agentRateLimiter;
    private final AgentMetrics          agentMetrics;
    private final CommunityAgentGraphService graphService;

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

    private StreamingCommunityAssistant buildStreamingAssistant(AgentFlowType flow) {
        boolean constrainedChannel = flow == AgentFlowType.CHANNEL;
        return AiServices.builder(StreamingCommunityAssistant.class)
                .streamingChatModel(models.streaming())
                .systemMessage(graphService.systemPromptFor(flow))
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

    public AgentAnswer answerSync(UUID workspaceId, UUID userId, String conversationId,
                                  String question, boolean constrainedChannel) {
        agentRateLimiter.consume(workspaceId);
        workspaceService.requireMembership(workspaceId, userId);

        String memId = (conversationId == null || conversationId.isBlank())
                ? "default-" + userId
                : conversationId;
        UUID convId = parseConversationId(conversationId);

        AgentRunStateHolder.init();
        AgentContextHolder.set(constrainedChannel
                ? AgentContext.channelBot(workspaceId, userId)
                : AgentContext.sync(workspaceId, userId, convId));

        try {
            String memories = longTermMemoryService.formatRecallForPrompt(
                    workspaceId, userId, convId, question);
            String composed = question;
            if (!memories.isBlank()) {
                composed = """
                        Relevant past Ask Bot conversation (vector search + this session):
                        %s

                        Current question:
                        %s
                        """.formatted(memories, question).trim();
                AgentRunState st = AgentRunStateHolder.get();
                if (st != null) {
                    st.addStep("conversation_memory", "Retrieved similar Ask Bot turns from vector store");
                }
            }

            String answer = graphService.runSync(composed, memId);

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
                longTermMemoryService.rememberExchange(workspaceId, userId, convId, question, answer);
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
        UUID convId = parseConversationId(conversationId);

        AgentRunStateHolder.init();

        java.util.function.Consumer<AgentStreamEvent> sink = ev -> {
            try {
                emitter.send(SseEmitter.event().name("agent").data(ev, MediaType.APPLICATION_JSON));
            } catch (Exception ex) {
                log.debug("SSE client disconnected: {}", ex.getMessage());
            }
        };

        AgentContextHolder.set(AgentContext.streaming(workspaceId, userId, convId, sink));

        try {
            emitter.send(SseEmitter.event().name("agent").data(
                    AgentStreamEvent.thinking("Searching past conversations and planning…"),
                    MediaType.APPLICATION_JSON));

            String memories = longTermMemoryService.formatRecallForPrompt(
                    workspaceId, userId, convId, question);
            String composed = question;
            if (!memories.isBlank()) {
                composed = """
                        Relevant past Ask Bot conversation (vector search + this session):
                        %s

                        Current question:
                        %s
                        """.formatted(memories, question).trim();
                AgentRunState st = AgentRunStateHolder.get();
                if (st != null) {
                    st.addStep("conversation_memory", "Retrieved similar Ask Bot turns from vector store");
                }
            }

            AgentFlowType flow = graphService.resolveFlowType();
            AgentRunState stEarly = AgentRunStateHolder.get();
            if (stEarly != null) {
                stEarly.addStep("flow_route", flow.name());
            }

            StreamingCommunityAssistant assistant = buildStreamingAssistant(flow);
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
                                longTermMemoryService.rememberExchange(
                                        workspaceId, userId, convId, question, text);
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
                            log.warn("Agent stream failed: {}", err.getMessage());
                            sink.accept(AgentStreamEvent.done(new AgentStreamEvent.AgentAnswerPayload(
                                    "Sorry, I could not complete that request. "
                                            + "Check that OPENAI_API_KEY is set and the backend logs for details.",
                                    0,
                                    List.of(),
                                    List.of(),
                                    null
                            )));
                            emitter.complete();
                        } catch (Exception e) {
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
            try {
                emitter.send(SseEmitter.event().name("agent").data(
                        AgentStreamEvent.done(new AgentStreamEvent.AgentAnswerPayload(
                                "Sorry, the assistant encountered an error: " + e.getMessage(),
                                0, List.of(), List.of(), null
                        )),
                        MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception sendErr) {
                emitter.completeWithError(e);
            }
        }
    }

    private static String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static UUID parseConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(conversationId.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
