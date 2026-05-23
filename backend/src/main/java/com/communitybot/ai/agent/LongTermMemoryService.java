package com.communitybot.ai.agent;

import com.communitybot.document.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * pgvector-backed Ask Bot conversation memory (workspace + user, grouped by conversation id).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LongTermMemoryService {

    private final EmbeddingClient embeddingClient;
    private final JdbcTemplate    jdbcTemplate;

    public record MemoryHit(String role, String content, Instant createdAt, UUID conversationId) {}

    /**
     * Builds RAG context from (1) recent turns in this Ask Bot session and
     * (2) semantically similar past turns across the workspace for this user.
     */
    public String formatRecallForPrompt(UUID workspaceId, UUID userId, UUID conversationId, String questionText) {
        float[]  emb = embeddingClient.embed(questionText);
        String vec = DocumentIngestionService.floatArrayToVectorString(emb);

        List<MemoryHit> hits = new ArrayList<>();
        if (conversationId != null) {
            hits.addAll(recentInConversation(workspaceId, userId, conversationId, 8));
        }
        hits.addAll(semanticSearch(workspaceId, userId, vec, 5));

        if (hits.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        Set<String> seen = new LinkedHashSet<>();
        for (MemoryHit hit : hits) {
            String line = formatHit(hit);
            if (seen.add(line)) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString().trim();
    }

    public List<String> search(UUID workspaceId, UUID userId, String vectorLiteral, int k) {
        return semanticSearch(workspaceId, userId, vectorLiteral, k).stream()
                .map(LongTermMemoryService::formatHit)
                .toList();
    }

    /** Exposed for agent tools. */
    public List<MemoryHit> semanticSearchForTool(UUID workspaceId, UUID userId, String vectorLiteral, int k) {
        return semanticSearch(workspaceId, userId, vectorLiteral, k);
    }

    /** Exposed for agent tools. */
    public List<MemoryHit> recentInConversationForTool(UUID workspaceId, UUID userId,
                                                        UUID conversationId, int limit) {
        return recentInConversation(workspaceId, userId, conversationId, limit);
    }

    /** Stores one user question and assistant answer as separate vectors for retrieval. */
    @Transactional
    public void rememberExchange(UUID workspaceId, UUID userId, UUID conversationId,
                                 String question, String answer) {
        if (question != null && !question.isBlank()) {
            indexTurn(workspaceId, userId, conversationId, "USER", question);
        }
        if (answer != null && !answer.isBlank()) {
            indexTurn(workspaceId, userId, conversationId, "ASSISTANT", answer);
        }
    }

    /** @deprecated use {@link #rememberExchange} */
    @Deprecated
    @Transactional
    public void rememberTurn(UUID workspaceId, UUID userId, String question, String answer) {
        rememberExchange(workspaceId, userId, null, question, answer);
    }

    private void indexTurn(UUID workspaceId, UUID userId, UUID conversationId,
                           String role, String content) {
        float[] emb = embeddingClient.embed(content);
        String vec  = DocumentIngestionService.floatArrayToVectorString(emb);
        UUID id     = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO agent_memories (id, workspace_id, user_id, conversation_id, kind, role, content, embedding)
                VALUES (?, ?, ?, ?, 'ASK_TURN', ?, ?, ?::vector)
                """,
                id, workspaceId, userId, conversationId, role, truncate(content, 2000), vec
        );
    }

    private List<MemoryHit> recentInConversation(UUID workspaceId, UUID userId,
                                                  UUID conversationId, int limit) {
        String sql = """
                SELECT role, content, created_at, conversation_id
                FROM agent_memories
                WHERE workspace_id = ? AND user_id = ? AND conversation_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """;
        List<MemoryHit> rows = jdbcTemplate.query(sql, (rs, i) -> new MemoryHit(
                rs.getString(1),
                rs.getString(2),
                rs.getTimestamp(3).toInstant(),
                rs.getObject(4, UUID.class)
        ), workspaceId, userId, conversationId, limit);
        // Return chronological order for readability
        return rows.reversed();
    }

    private List<MemoryHit> semanticSearch(UUID workspaceId, UUID userId,
                                           String vectorLiteral, int k) {
        String sql = """
                SELECT role, content, created_at, conversation_id
                FROM agent_memories
                WHERE workspace_id = ? AND user_id = ?
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, i) -> new MemoryHit(
                rs.getString(1),
                rs.getString(2),
                rs.getTimestamp(3).toInstant(),
                rs.getObject(4, UUID.class)
        ), workspaceId, userId, vectorLiteral, k);
    }

    private static String formatHit(MemoryHit hit) {
        String role = hit.role() != null ? hit.role() : "MEMORY";
        return "[" + role + "] " + truncate(hit.content(), 700);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /** Thin wrapper to avoid a direct dependency cycle with the existing EmbeddingService. */
    @FunctionalInterface
    public interface EmbeddingClient {
        float[] embed(String text);
    }
}
