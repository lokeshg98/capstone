package com.communitybot.ai.agent;

import com.communitybot.ai.config.AgentProperties;
import com.communitybot.ai.service.EmbeddingService;
import com.communitybot.channel.service.ChannelService;
import com.communitybot.document.repository.DocumentChunkRepository;
import com.communitybot.document.repository.FaqSearchChunkRow;
import com.communitybot.document.service.DocumentIngestionService;
import com.communitybot.message.domain.Message;
import com.communitybot.message.domain.MessageStatus;
import com.communitybot.message.repository.MessageRepository;
import com.communitybot.message.service.MessageEmbeddingService;
import com.communitybot.message.service.ThreadSummaryService;
import com.communitybot.moderation.domain.FlagStatus;
import com.communitybot.moderation.dto.ModerationFlagResponse;
import com.communitybot.moderation.repository.ModerationFlagRepository;
import com.communitybot.moderation.service.ModerationService;
import com.communitybot.workspace.domain.WorkspaceMember;
import com.communitybot.workspace.repository.WorkspaceMemberRepository;
import com.communitybot.workspace.service.BanService;
import com.communitybot.workspace.service.BanService.BanResponse;
import com.communitybot.workspace.service.WorkspaceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchResults;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Tool implementations for the multi-agent orchestrator.
 * All methods resolve workspace/user from {@link AgentContextHolder}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommunityAgentTools {

    private static final List<MessageStatus> HIDDEN = List.of(
            MessageStatus.DELETED, MessageStatus.HIDDEN);

    private final DocumentChunkRepository    documentChunkRepository;
    private final LongTermMemoryService      longTermMemoryService;
    private final EmbeddingService           embeddingService;
    private final MessageRepository          messageRepository;
    private final MessageEmbeddingService    messageEmbeddingService;
    private final ThreadSummaryService       threadSummaryService;
    private final ChannelService             channelService;
    private final WorkspaceService           workspaceService;
    private final WorkspaceMemberRepository  workspaceMemberRepository;
    private final ModerationService          moderationService;
    private final ModerationFlagRepository   moderationFlagRepository;
    private final BanService                 banService;
    private final ProposedActionRepository   proposedActionRepository;
    private final AgentProperties            agentProperties;
    private final ChatModel                  chatModel;
    private final ObjectMapper               objectMapper;

    private WebSearchEngine webSearchOrNull() {
        String k = agentProperties.getTavilyApiKey();
        if (k == null || k.isBlank()) {
            return null;
        }
        return TavilyWebSearchEngine.withApiKey(k);
    }

    @Tool("Search ingested FAQ / PDF and DOCX knowledge for this workspace. Pass a short search query.")
    public String searchFaqDocuments(String query) {
        AgentContext ctx = requireContext();
        emitStep("faq_search", query);
        try {
            float[] emb = embeddingService.embed(query);
            String vec = DocumentIngestionService.floatArrayToVectorString(emb);
            List<FaqSearchChunkRow> rows = documentChunkRepository.findTopFaqChunksWithTitles(ctx.workspaceId(), vec);
            log.info("searchFaqDocuments: query='{}' workspaceId={} rowsFound={}", query, ctx.workspaceId(), rows.size());
            if (rows.isEmpty()) {
                return "No FAQ chunks retrieved. The workspace may have no ingested documents yet.";
            }
            AgentRunState state = AgentRunStateHolder.get();
            StringJoiner joiner = new StringJoiner("\n\n---\n\n");
            for (FaqSearchChunkRow row : rows) {
                if (state != null) {
                    state.addCitation(row.getDocumentTitle(), row.getChunkText(), row.getChunkIndex());
                }
                joiner.add("Document: " + row.getDocumentTitle() + "\n" + row.getChunkText());
            }
            return joiner.toString();
        } catch (Exception e) {
            log.warn("FAQ search failed: {}", e.getMessage());
            return "FAQ search failed: " + e.getMessage();
        }
    }

    @Tool("Search past Ask Bot conversations stored in the vector database (pgvector). "
            + "Finds semantically similar Q&A from this user in this workspace.")
    public String recallPastConversations(String query) {
        AgentContext ctx = requireContext();
        if (ctx.constrainedChannelMode()) {
            return "Conversation memory search is not available in channel mode. Use searchFaqDocuments instead.";
        }
        emitStep("memory_recall", query);
        try {
            float[] emb = embeddingService.embed(query);
            String vec = DocumentIngestionService.floatArrayToVectorString(emb);
            int k = agentProperties.getLongTermMemoryTopK();
            List<LongTermMemoryService.MemoryHit> hits = new ArrayList<>();
            if (ctx.conversationId() != null) {
                hits.addAll(longTermMemoryService.recentInConversationForTool(
                        ctx.workspaceId(), ctx.userId(), ctx.conversationId(), 6));
            }
            hits.addAll(longTermMemoryService.semanticSearchForTool(
                    ctx.workspaceId(), ctx.userId(), vec, k));
            if (hits.isEmpty()) {
                return "No matching past Ask Bot conversations in vector memory.";
            }
            StringJoiner j = new StringJoiner("\n---\n");
            Set<String> seen = new LinkedHashSet<>();
            for (LongTermMemoryService.MemoryHit hit : hits) {
                String line = "[" + (hit.role() != null ? hit.role() : "MEMORY") + "] " + truncate(hit.content(), 500);
                if (seen.add(line)) {
                    j.add(line);
                }
            }
            return j.toString();
        } catch (Exception e) {
            return "Memory recall failed: " + e.getMessage();
        }
    }

    @Tool("Search recent messages in one channel by keyword (case-insensitive). channelId must be a UUID string.")
    public String searchChannelMessagesByKeyword(String channelId, String keyword, int limit) {
        AgentContext ctx = requireContext();
        emitStep("channel_keyword", channelId + " | " + keyword);
        UUID chId = parseUuid(channelId);
        if (chId == null) return "Invalid channelId (expected UUID).";
        if (ctx.constrainedChannelMode()) {
            return "Channel search is not available in quick @bot replies. Open Ask Bot for full search.";
        }
        try {
            workspaceService.requireMembership(ctx.workspaceId(), ctx.userId());
            var channel = channelService.getOrThrow(chId);
            if (!channel.getWorkspace().getId().equals(ctx.workspaceId())) {
                return "Channel is not in this workspace.";
            }
        } catch (Exception e) {
            return "Access denied or channel not in workspace: " + e.getMessage();
        }
        int lim = Math.clamp(limit, 1, 30);
        List<Message> hits = messageRepository.searchByChannelKeyword(
                chId, ctx.workspaceId(), keyword, HIDDEN, PageRequest.of(0, lim));
        if (hits.isEmpty()) return "No messages matched in that channel.";
        StringJoiner j = new StringJoiner("\n");
        for (Message m : hits) {
            j.add(m.getCreatedAt() + " — " + truncate(m.getBody(), 240));
        }
        return j.toString();
    }

    @Tool("Semantic search across all indexed channel messages in this workspace.")
    public String searchWorkspaceMessagesSemantic(String query, int limit) {
        AgentContext ctx = requireContext();
        emitStep("semantic_msgs", query);
        if (ctx.constrainedChannelMode()) {
            return "Semantic message search is not available in quick @bot replies.";
        }
        try {
            workspaceService.requireMembership(ctx.workspaceId(), ctx.userId());
            float[] emb = embeddingService.embed(query);
            String vec = DocumentIngestionService.floatArrayToVectorString(emb);
            int lim = Math.clamp(limit, 1, 20);
            List<MessageEmbeddingService.SemanticHit> found =
                    messageEmbeddingService.semanticSearch(ctx.workspaceId(), vec, lim);
            if (found.isEmpty()) {
                return "No similar indexed messages found (index fills as new messages arrive).";
            }
            StringJoiner j = new StringJoiner("\n");
            for (MessageEmbeddingService.SemanticHit h : found) {
                j.add("msg=" + h.messageId() + " channel=" + h.channelId() + " — " + truncate(h.body(), 200));
            }
            return j.toString();
        } catch (Exception e) {
            return "Semantic search failed: " + e.getMessage();
        }
    }

    @Tool("Fetch a transcript for a message thread. threadRootId is the root message UUID.")
    public String fetchThreadTranscript(String threadRootId) {
        AgentContext ctx = requireContext();
        emitStep("thread_fetch", threadRootId);
        return buildThreadTranscript(ctx, threadRootId);
    }

    @Tool("Summarize a discussion thread (root message UUID). Returns Otter-style overview sections.")
    public String summarizeThread(String threadRootId) {
        AgentContext ctx = requireContext();
        emitStep("thread_summarize", threadRootId);
        if (ctx.constrainedChannelMode()) {
            return "Thread summarization is not available in quick @bot replies.";
        }
        UUID rootId = parseUuid(threadRootId);
        if (rootId == null) return "Invalid thread root id.";
        var rootOpt = messageRepository.findById(rootId);
        if (rootOpt.isEmpty()) return "Thread root message not found.";
        Message root = rootOpt.get();
        if (!root.getWorkspaceId().equals(ctx.workspaceId())) {
            return "Thread belongs to another workspace.";
        }
        return threadSummaryService.summarizeOnDemand(rootId, ctx.userId());
    }

    private String buildThreadTranscript(AgentContext ctx, String threadRootId) {
        UUID rootId = parseUuid(threadRootId);
        if (rootId == null) return "Invalid thread root id.";
        var rootOpt = messageRepository.findById(rootId);
        if (rootOpt.isEmpty()) return "Thread root message not found.";
        Message root = rootOpt.get();
        if (!root.getWorkspaceId().equals(ctx.workspaceId())) {
            return "That message belongs to another workspace.";
        }
        List<Message> replies = messageRepository.findThreadReplies(rootId, HIDDEN);
        StringBuilder sb = new StringBuilder();
        sb.append("ROOT: ").append(root.getBody()).append("\n");
        for (Message r : replies) {
            sb.append("— ").append(r.getBody()).append("\n");
        }
        return sb.toString();
    }

    @Tool("Search the public web for fresh information (Tavily). Use sparingly; requires admin when configured.")
    public String searchWeb(String query) {
        AgentContext ctx = requireContext();
        emitStep("web_search", query);
        if (ctx.constrainedChannelMode()) {
            return "Web search is disabled in quick @bot replies.";
        }
        WebSearchEngine engine = webSearchOrNull();
        if (engine == null) {
            return "Web search is not enabled for this workspace. Ask a workspace admin if you need live web results.";
        }
        if (agentProperties.isWebSearchAdminOnly()) {
            WorkspaceMember member = workspaceMemberRepository
                    .findByWorkspaceIdAndUserId(ctx.workspaceId(), ctx.userId())
                    .orElse(null);
            if (member == null || !member.hasRole("Admin")) {
                return "Web search is limited to workspace admins in this deployment.";
            }
        }
        try {
            WebSearchResults results = engine.search(WebSearchRequest.from(query));
            var segments = results.toTextSegments();
            if (segments.isEmpty()) {
                return "No web results.";
            }
            StringJoiner j = new StringJoiner("\n\n");
            for (var seg : segments.stream().limit(6).toList()) {
                j.add(seg.text());
            }
            return j.toString();
        } catch (Exception e) {
            log.warn("Web search failed: {}", e.getMessage());
            return "Web search failed: " + e.getMessage();
        }
    }

    @Tool("List moderation flags. statusFilter: PENDING, APPROVED, REMOVED, or ALL.")
    public String listModerationFlags(String statusFilter) {
        AgentContext ctx = requireContext();
        emitStep("mod_flags", statusFilter);
        try {
            String f = statusFilter == null || statusFilter.isBlank() ? null
                    : ("ALL".equalsIgnoreCase(statusFilter) ? null : statusFilter.toUpperCase(Locale.ROOT));
            List<ModerationFlagResponse> flags =
                    moderationService.listFlags(ctx.workspaceId(), f, ctx.userId());
            if (flags.isEmpty()) return "No moderation flags for this filter.";
            StringJoiner j = new StringJoiner("\n");
            for (ModerationFlagResponse fl : flags.stream().limit(25).toList()) {
                j.add(fl.flagId() + " status=" + fl.status() + " reason=" + fl.llmReason()
                        + " conf=" + fl.llmConfidence());
            }
            return j.toString();
        } catch (Exception e) {
            return "Cannot list moderation flags (role or server error): " + e.getMessage();
        }
    }

    @Tool("List active workspace bans (moderator view).")
    public String listWorkspaceBans() {
        AgentContext ctx = requireContext();
        emitStep("mod_bans", "");
        try {
            List<BanResponse> bans = banService.listActiveBans(ctx.workspaceId(), ctx.userId());
            if (bans.isEmpty()) return "No active bans.";
            StringJoiner j = new StringJoiner("\n");
            for (BanResponse b : bans) {
                j.add("user=" + b.userId() + " reason=" + b.reason());
            }
            return j.toString();
        } catch (Exception e) {
            return "Cannot list bans: " + e.getMessage();
        }
    }

    @Tool("Moderation stats for the last N days: pending/approved/removed counts.")
    public String moderationStats(int lastDays) {
        AgentContext ctx = requireContext();
        emitStep("mod_stats", String.valueOf(lastDays));
        try {
            workspaceService.requireModeratorOrAdmin(ctx.workspaceId(), ctx.userId());
        } catch (Exception e) {
            return "Moderation stats require moderator or admin role.";
        }
        int days = Math.clamp(lastDays, 1, 90);
        Instant since = Instant.now().minusSeconds(days * 86400L);
        long pending = moderationFlagRepository.countByWorkspaceIdAndStatus(ctx.workspaceId(), FlagStatus.PENDING);
        long approved = moderationFlagRepository.countByWorkspaceIdAndStatus(ctx.workspaceId(), FlagStatus.APPROVED);
        long removed = moderationFlagRepository.countByWorkspaceIdAndStatus(ctx.workspaceId(), FlagStatus.REMOVED);
        long recent = moderationFlagRepository.countByWorkspaceIdAndCreatedAtGreaterThanEqual(ctx.workspaceId(), since);
        return "Last " + days + "d new flags (all statuses): " + recent
                + "\nCurrent totals — PENDING=" + pending + " APPROVED=" + approved + " REMOVED=" + removed;
    }

    @Tool("Propose a scheduled channel post. Human must confirm in the UI before it is created. "
            + "scheduleType is ONE_SHOT or CRON. For ONE_SHOT provide fireAt as ISO-8601 instant. "
            + "For CRON provide Spring 6-field cron in cronExpression.")
    public String proposeScheduledPost(
            String channelId,
            String body,
            String scheduleType,
            String fireAtIso,
            String cronExpression
    ) {
        AgentContext ctx = requireContext();
        emitStep("schedule_propose", channelId);
        if (ctx.constrainedChannelMode()) {
            return "Scheduling proposals are disabled in quick @bot replies.";
        }
        UUID ch = parseUuid(channelId);
        if (ch == null) return "Invalid channel id.";
        try {
            workspaceService.requireMembership(ctx.workspaceId(), ctx.userId());
            var channel = channelService.getOrThrow(ch);
            if (!channel.getWorkspace().getId().equals(ctx.workspaceId())) {
                return "Channel is not in this workspace.";
            }
        } catch (Exception e) {
            return "Cannot access channel: " + e.getMessage();
        }
        var payload = new ScheduleProposalPayload(ch, body, scheduleType, fireAtIso, cronExpression);
        try {
            String json = objectMapper.writeValueAsString(payload);
            ProposedAction saved = proposedActionRepository.save(
                    ProposedAction.builder()
                            .workspaceId(ctx.workspaceId())
                            .userId(ctx.userId())
                            .actionType("SCHEDULED_POST")
                            .payloadJson(json)
                            .build()
            );
            AgentRunState state = AgentRunStateHolder.get();
            if (state != null) {
                state.setProposal(saved.getId(), "SCHEDULED_POST",
                        truncate(body, 120) + " @ " + scheduleType);
            }
            ctx.emit(AgentStreamEvent.actionProposal(new AgentStreamEvent.AgentAnswerPayload(
                    null,
                    0,
                    List.of(),
                    List.of(),
                    new AgentStreamEvent.ProposalPayload(saved.getId(), "SCHEDULED_POST", truncate(body, 200))
            )));
            return "Proposal recorded as " + saved.getId()
                    + ". The user must click Confirm in Ask Bot before it is scheduled.";
        } catch (Exception e) {
            return "Failed to record proposal: " + e.getMessage();
        }
    }

    private static AgentContext requireContext() {
        AgentContext ctx = AgentContextHolder.get();
        if (ctx == null) {
            throw new IllegalStateException("AgentContext not set");
        }
        return ctx;
    }

    private static void emitStep(String kind, String detail) {
        AgentRunState state = AgentRunStateHolder.get();
        if (state != null) {
            state.addStep(kind, detail);
        }
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
