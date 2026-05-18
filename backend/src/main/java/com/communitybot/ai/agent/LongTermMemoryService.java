package com.communitybot.ai.agent;

import com.communitybot.document.service.DocumentIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * pgvector-backed long-term memory scoped to (workspace, user).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LongTermMemoryService {

    private final EmbeddingClient embeddingClient;
    private final JdbcTemplate    jdbcTemplate;

    /** Loads top-K past summaries relevant to the question text. */
    public String formatRecallForPrompt(UUID workspaceId, UUID userId, String questionText) {
        float[]  emb = embeddingClient.embed(questionText);
        String vec = DocumentIngestionService.floatArrayToVectorString(emb);
        List<String> lines = search(workspaceId, userId, vec, 3);
        if (lines.isEmpty()) return "";
        return String.join("\n", lines);
    }

    public List<String> search(UUID workspaceId, UUID userId, String vectorLiteral, int k) {
        String sql = """
                SELECT content FROM agent_memories
                WHERE workspace_id = ? AND user_id = ?
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, i) -> rs.getString(1),
                workspaceId, userId, vectorLiteral, k);
    }

    @Transactional
    public void rememberTurn(UUID workspaceId, UUID userId, String question, String answer) {
        String summary = "Q: " + truncate(question, 400) + "\nA: " + truncate(answer, 600);
        float[] emb  = embeddingClient.embed(summary);
        String vec   = DocumentIngestionService.floatArrayToVectorString(emb);
        UUID   id    = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO agent_memories (id, workspace_id, user_id, kind, content, embedding)
                VALUES (?, ?, ?, 'TURN_SUMMARY', ?, ?::vector)
                """,
                id, workspaceId, userId, summary, vec
        );
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
