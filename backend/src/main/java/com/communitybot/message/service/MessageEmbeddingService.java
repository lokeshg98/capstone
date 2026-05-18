package com.communitybot.message.service;

import com.communitybot.document.service.DocumentIngestionService;
import com.communitybot.message.domain.Message;
import com.communitybot.message.domain.MessageStatus;
import com.communitybot.message.repository.MessageRepository;
import com.communitybot.ai.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageEmbeddingService {

    private static final List<MessageStatus> EXCLUDE = List.of(MessageStatus.DELETED, MessageStatus.HIDDEN);

    private final JdbcTemplate        jdbcTemplate;
    private final MessageRepository   messageRepository;
    private final EmbeddingService    embeddingService;

    @Transactional
    public void indexMessage(UUID messageId, UUID workspaceId) {
        Message msg = messageRepository.findById(messageId).orElse(null);
        if (msg == null) return;
        if (EXCLUDE.contains(msg.getStatus())) return;
        String body = msg.getBody();
        if (body == null || body.isBlank()) return;

        float[] emb = embeddingService.embed(body, msg.getAuthor().getId());
        String vec = DocumentIngestionService.floatArrayToVectorString(emb);
        UUID rowId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO message_embeddings (id, message_id, workspace_id, embedding)
                VALUES (?, ?, ?, ?::vector)
                ON CONFLICT (message_id) DO UPDATE SET embedding = EXCLUDED.embedding, workspace_id = EXCLUDED.workspace_id
                """,
                rowId, messageId, workspaceId, vec
        );
    }

    public List<SemanticHit> semanticSearch(UUID workspaceId, String queryVectorLiteral, int limit) {
        String sql = """
                SELECT m.id, m.body, m.channel_id
                FROM message_embeddings me
                JOIN messages m ON m.id = me.message_id
                WHERE me.workspace_id = ?
                  AND m.status NOT IN ('DELETED', 'HIDDEN')
                ORDER BY me.embedding <=> CAST(? AS vector)
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, i) -> new SemanticHit(
                rs.getObject(1, UUID.class),
                rs.getString(2),
                rs.getObject(3, UUID.class)
        ), workspaceId, queryVectorLiteral, limit);
    }

    public record SemanticHit(UUID messageId, String body, UUID channelId) {}
}
