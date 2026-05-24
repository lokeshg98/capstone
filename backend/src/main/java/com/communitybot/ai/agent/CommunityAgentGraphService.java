package com.communitybot.ai.agent;

import com.communitybot.workspace.repository.WorkspaceMemberRepository;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphDefinition;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.agentexecutor.AgentExecutor;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * LangGraph4j orchestrator: routes each request through a flow-resolution node,
 * then into a role-specific AgentExecutor subgraph (public / member / moderator / channel).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityAgentGraphService {

    private static final String SYSTEM_PUBLIC = """
            You are Community Bot on the public marketing site. Visitors are not logged in.
            Use searchPublicFaq to answer questions about the platform, features, and getting started.
            Be concise and friendly. If they should sign in for workspace features, say so with a clear CTA.
            """ + AgentUserFacingRules.SYSTEM_APPENDIX;

    private static final String SYSTEM_MEMBER = """
            You are Community Bot for a workspace member (not a moderator).
            Past Ask Bot conversations are stored and relevant excerpts are injected into your prompt
            automatically.

            IMPORTANT: When the user asks a factual or knowledge-based question (especially about rules,
            policies, or any documented information), call searchFaqDocuments ONCE. Then answer directly
            from the returned chunks. Do NOT call additional tools after searchFaqDocuments — the chunks
            contain everything you need. If the chunks do not answer the question, say so politely.

            You cannot access moderation tools, web search, or schedule posts — explain that politely if asked.
            """ + AgentUserFacingRules.SYSTEM_APPENDIX;

    private static final String SYSTEM_MODERATOR = """
            You are Community Bot for workspace moderators and admins.
            Past Ask Bot conversations are stored and auto-retrieved for context.

            IMPORTANT: When the user asks a factual question, call searchFaqDocuments. Answer from the
            returned chunks — do not keep calling tools. Use moderation tools, web search, or scheduled
            posts only when specifically requested.
            Keep replies helpful and concise.
            """ + AgentUserFacingRules.SYSTEM_APPENDIX;

    private static final String SYSTEM_CHANNEL = """
            You are Community Bot answering a @bot mention in a channel thread.
            You have EXACTLY ONE tool available: searchFaqDocuments. Call it with a short query,
            then answer the user's question using ONLY the document excerpts returned. Be brief.
            """ + AgentUserFacingRules.SYSTEM_APPENDIX;

    private final MultiAgentService.ChatModelBridge models;
    private final CommunityAgentTools             workspaceTools;
    private final ChannelAgentTools               channelTools;
    private final PublicAgentTools                publicTools;
    private final WorkspaceMemberRepository       workspaceMemberRepository;
    private final UserFacingResponseSanitizer   responseSanitizer;

    private CompiledGraph<AgentExecutor.State> orchestrator;
    private CompiledGraph<AgentExecutor.State> publicAgent;
    private CompiledGraph<AgentExecutor.State> memberAgent;
    private CompiledGraph<AgentExecutor.State> moderatorAgent;
    private CompiledGraph<AgentExecutor.State> channelAgent;

    @PostConstruct
    void initGraph() throws GraphStateException {
        ChatModel chatModel = models.chat();

        this.publicAgent = compileAgent(chatModel, publicTools, SYSTEM_PUBLIC);
        this.memberAgent = compileAgent(chatModel, workspaceTools, SYSTEM_MEMBER);
        this.moderatorAgent = compileAgent(chatModel, workspaceTools, SYSTEM_MODERATOR);
        this.channelAgent = compileAgent(chatModel, channelTools, SYSTEM_CHANNEL);

        StateGraph<AgentExecutor.State> graph = new StateGraph<>(
                MessagesState.SCHEMA,
                AgentExecutor.State::new
        )
                .addNode(AgentFlowType.PUBLIC.nodeName(), publicAgent)
                .addNode(AgentFlowType.MEMBER.nodeName(), memberAgent)
                .addNode(AgentFlowType.MODERATOR.nodeName(), moderatorAgent)
                .addNode(AgentFlowType.CHANNEL.nodeName(), channelAgent)
                .addConditionalEdges(
                        GraphDefinition.START,
                        state -> CompletableFuture.completedFuture(
                                AgentFlowType.fromContext(
                                        AgentContextHolder.get(), workspaceMemberRepository).nodeName()),
                        Map.of(
                                AgentFlowType.PUBLIC.nodeName(),    AgentFlowType.PUBLIC.nodeName(),
                                AgentFlowType.MEMBER.nodeName(),    AgentFlowType.MEMBER.nodeName(),
                                AgentFlowType.MODERATOR.nodeName(), AgentFlowType.MODERATOR.nodeName(),
                                AgentFlowType.CHANNEL.nodeName(),   AgentFlowType.CHANNEL.nodeName()
                        )
                )
                .addEdge(AgentFlowType.PUBLIC.nodeName(),    GraphDefinition.END)
                .addEdge(AgentFlowType.MEMBER.nodeName(),    GraphDefinition.END)
                .addEdge(AgentFlowType.MODERATOR.nodeName(), GraphDefinition.END)
                .addEdge(AgentFlowType.CHANNEL.nodeName(),   GraphDefinition.END);

        this.orchestrator = graph.compile(CompileConfig.builder().build());
        log.info("LangGraph4j agent orchestrator compiled (public / member / moderator / channel flows)");
    }

    public AgentFlowType resolveFlowType() {
        return AgentFlowType.fromContext(AgentContextHolder.get(), workspaceMemberRepository);
    }

    public String systemPromptFor(AgentFlowType flow) {
        return switch (flow) {
            case PUBLIC    -> SYSTEM_PUBLIC;
            case CHANNEL   -> SYSTEM_CHANNEL;
            case MEMBER    -> SYSTEM_MEMBER;
            case MODERATOR -> SYSTEM_MODERATOR;
        };
    }

    public String runSync(String question, String memoryId) {
        AgentFlowType flow = resolveFlowType();
        AgentRunState stEarly = AgentRunStateHolder.get();
        if (stEarly != null) {
            stEarly.addStep("flow_route", flow.name());
        }

        CompiledGraph<AgentExecutor.State> agent = switch (flow) {
            case PUBLIC    -> publicAgent;
            case CHANNEL   -> channelAgent;
            case MEMBER    -> memberAgent;
            case MODERATOR -> moderatorAgent;
        };

        // Invoke the role-specific subgraph directly (orchestrator nesting breaks message serialization).
        Map<String, Object> input = Map.of("messages", UserMessage.from(question));
        RunnableConfig config = RunnableConfig.builder()
                .threadId(memoryId == null ? "anon" : memoryId)
                .build();

        try {
            Optional<AgentExecutor.State> state = agent.invoke(input, config);
            if (state.isEmpty()) {
                return "I couldn't produce an answer. Please try again.";
            }
            return responseSanitizer.sanitize(state.get().finalResponse()
                    .filter(s -> !s.isBlank())
                    .orElseGet(() -> lastAiText(state.get())));
        } catch (Exception e) {
            log.error("LangGraph4j agent run failed (flow={})", flow, e);
            throw new RuntimeException("Agent execution failed: " + e.getMessage(), e);
        }
    }

    private CompiledGraph<AgentExecutor.State> compileAgent(
            ChatModel chatModel, Object tools, String systemPrompt
    ) throws GraphStateException {
        return AgentExecutor.builder()
                .chatModel(chatModel)
                .toolSpecification(tools)
                .systemMessage(SystemMessage.from(systemPrompt))
                .stateSerializer(AgentExecutor.Serializers.STD.object())
                .build()
                .compile();
    }

    private static String lastAiText(AgentExecutor.State state) {
        return state.messages().stream()
                .filter(m -> m instanceof dev.langchain4j.data.message.AiMessage)
                .map(m -> ((dev.langchain4j.data.message.AiMessage) m).text())
                .reduce((a, b) -> b)
                .orElse("");
    }
}
