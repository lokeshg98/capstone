package com.communitybot.ai.service;

import com.communitybot.ai.config.OpenAiProperties;
import com.communitybot.document.domain.DocumentChunkProjection;
import com.communitybot.document.repository.DocumentChunkRepository;
import com.communitybot.document.service.DocumentIngestionService;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Retrieval-Augmented Generation pipeline:
 * <ol>
 *   <li>Rate-limit check (Redis counter per workspace per day)</li>
 *   <li>Embed the question with {@code text-embedding-3-small}</li>
 *   <li>Cosine-similarity search in pgvector for the top-5 chunks</li>
 *   <li>Build a context-stuffed prompt and call {@code gpt-4o-mini}</li>
 * </ol>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingService        embeddingService;
    private final OpenAiChatService       chatService;
    private final DocumentChunkRepository chunkRepository;
    private final StringRedisTemplate     redisTemplate;
    private final OpenAiProperties        props;

    private static final String SYSTEM_PROMPT = """
            You are a helpful community assistant.
            Answer the user's question using ONLY the context excerpts provided below.
            If the context does not contain enough information to answer, say exactly:
            "I don't have information about that in the FAQ documents."
            Be concise, friendly, and accurate. Do not fabricate facts.
            """;

    public RagResult ask(UUID workspaceId, String question) {
        enforceRateLimit(workspaceId);

        // 1. Embed the question
        float[]  queryEmbedding = embeddingService.embed(question);
        String   vectorStr      = DocumentIngestionService.floatArrayToVectorString(queryEmbedding);

        // 2. Retrieve top-K chunks by cosine similarity
        List<DocumentChunkProjection> chunks = chunkRepository.findTopKByWorkspace(workspaceId, vectorStr);

        if (chunks.isEmpty()) {
            return new RagResult(
                    "This workspace has no FAQ documents yet. " +
                    "A workspace admin can upload PDF or DOCX files under the Documents section.",
                    0
            );
        }

        // 3. Build context string from retrieved chunks
        StringBuilder context = new StringBuilder();
        for (DocumentChunkProjection chunk : chunks) {
            context.append("---\n").append(chunk.getChunkText()).append('\n');
        }

        // 4. Call the LLM
        List<OpenAiChatService.ChatMessage> messages = List.of(
                OpenAiChatService.ChatMessage.system(SYSTEM_PROMPT + "\n\nContext:\n" + context),
                OpenAiChatService.ChatMessage.user(question)
        );

        String answer = chatService.complete(messages);
        log.info("RAG answered for workspace={} question='{}' sourceChunks={}",
                workspaceId, question.length() > 60 ? question.substring(0, 60) + "…" : question,
                chunks.size());
        return new RagResult(answer, chunks.size());
    }

    public record RagResult(String answer, int sourceChunks) {}

    // ── Cost guardrail ────────────────────────────────────────────────────────

    private void enforceRateLimit(UUID workspaceId) {
        String key   = "rag:daily:" + workspaceId + ":" + LocalDate.now();
        Long   count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            // First call today — set 25-hour TTL so the key always expires
            redisTemplate.expire(key, Duration.ofHours(25));
        }
        if (count != null && count > props.getDailyRagLimit()) {
            throw new AppException(ErrorCode.RAG_RATE_LIMIT_EXCEEDED);
        }
    }
}
