package com.communitybot.document.service;

import com.communitybot.attachment.domain.Attachment;
import com.communitybot.attachment.service.AttachmentService;
import com.communitybot.document.domain.DocumentChunk;
import com.communitybot.document.domain.FaqDocument;
import com.communitybot.document.dto.FaqDocumentResponse;
import com.communitybot.document.repository.DocumentChunkRepository;
import com.communitybot.document.repository.FaqDocumentRepository;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import com.communitybot.workspace.domain.Workspace;
import com.communitybot.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final AttachmentService       attachmentService;
    private final TextExtractorService    textExtractor;
    private final ChunkingService         chunkingService;
    private final FaqDocumentRepository   faqDocumentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final WorkspaceService        workspaceService;
    private final JdbcTemplate            jdbcTemplate;

    /**
     * Full ingestion pipeline for a previously uploaded attachment:
     * <ol>
     *   <li>Extract plain text (PDF → PDFBox, DOCX → POI)</li>
     *   <li>Split into overlapping windows</li>
     *   <li>Embed each chunk via OpenAI</li>
     *   <li>Persist chunks with their vectors</li>
     * </ol>
     * The {@code embeddingFn} parameter accepts a String and returns {@code float[]}
     * to keep this service decoupled from the AI module (avoids circular spring deps).
     */
    @Transactional
    public FaqDocumentResponse ingest(
            UUID attachmentId,
            UUID workspaceId,
            UUID userId,
            java.util.function.Function<String, float[]> embeddingFn
    ) {
        workspaceService.requireMembership(workspaceId, userId);
        Workspace  workspace  = workspaceService.getOrThrow(workspaceId);
        Attachment attachment = attachmentService.getOrThrow(attachmentId);

        // 1. Extract text
        String rawText;
        try (InputStream stream = attachmentService.openStream(attachment)) {
            rawText = textExtractor.extract(stream, attachment.getKind());
        } catch (IOException e) {
            log.error("Text extraction failed for attachment {}: {}", attachmentId, e.getMessage());
            throw new AppException(ErrorCode.DOCUMENT_INGESTION_FAILED);
        }

        if (rawText.isBlank()) {
            throw new AppException(ErrorCode.DOCUMENT_INGESTION_FAILED);
        }

        // 2. Persist the FAQ document record
        FaqDocument faqDoc = faqDocumentRepository.save(
                FaqDocument.builder()
                        .workspace(workspace)
                        .attachment(attachment)
                        .title(attachment.getFilename())
                        .build()
        );

        // 3. Chunk the text
        List<String> chunks = chunkingService.chunk(rawText);
        log.info("Ingesting '{}': {} chunks for workspace {}", attachment.getFilename(), chunks.size(), workspaceId);

        // 4. Embed each chunk and persist
        for (int i = 0; i < chunks.size(); i++) {
            String  chunkText = chunks.get(i);
            float[] embedding = embeddingFn.apply(chunkText);

            // Save the chunk entity (without embedding — pgvector requires a raw JDBC update)
            DocumentChunk saved = chunkRepository.save(
                    DocumentChunk.builder()
                            .faqDocument(faqDoc)
                            .workspaceId(workspaceId)
                            .chunkText(chunkText)
                            .chunkIndex(i)
                            .build()
            );

            // Write the vector via raw JDBC using pgvector's ::vector cast
            jdbcTemplate.update(
                    "UPDATE document_chunks SET embedding = ?::vector WHERE id = ?",
                    floatArrayToVectorString(embedding),
                    saved.getId()
            );
        }

        faqDoc.recordChunkCount(chunks.size());
        return FaqDocumentResponse.from(faqDoc);
    }

    // -------------------------------------------------------------------------

    public static String floatArrayToVectorString(float[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(arr[i]);
        }
        return sb.append(']').toString();
    }
}
