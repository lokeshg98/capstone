package com.communitybot.document.controller;

import com.communitybot.ai.service.EmbeddingService;
import com.communitybot.document.dto.FaqDocumentResponse;
import com.communitybot.document.repository.DocumentChunkRepository;
import com.communitybot.document.repository.FaqDocumentRepository;
import com.communitybot.document.service.DocumentIngestionService;
import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.security.CurrentUser;
import com.communitybot.workspace.service.WorkspaceService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{wsId}/documents")
@RequiredArgsConstructor
@Validated
public class DocumentController {

    private final DocumentIngestionService ingestionService;
    private final FaqDocumentRepository    faqDocumentRepository;
    private final DocumentChunkRepository  documentChunkRepository;
    private final EmbeddingService         embeddingService;
    private final WorkspaceService         workspaceService;

    /**
     * Ingests a previously uploaded attachment as a FAQ document.
     * The attachment must already be clean (passed the ClamAV scan in the upload step).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FaqDocumentResponse>> ingest(
            @PathVariable UUID wsId,
            @RequestBody IngestRequest req,
            @CurrentUser UUID userId
    ) {
        FaqDocumentResponse response = ingestionService.ingest(
                req.attachmentId(), wsId, userId, (text) -> embeddingService.embed(text, userId));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FaqDocumentResponse>>> list(
            @PathVariable UUID wsId,
            @CurrentUser UUID userId
    ) {
        List<FaqDocumentResponse> docs = faqDocumentRepository
                .findAllByWorkspaceIdOrderByCreatedAtDesc(wsId)
                .stream()
                .map(FaqDocumentResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(docs));
    }

    @DeleteMapping("/{documentId}")
    @Transactional
    public ResponseEntity<Void> delete(
            @PathVariable UUID wsId,
            @PathVariable UUID documentId,
            @CurrentUser UUID userId
    ) {
        workspaceService.requireModeratorOrAdmin(wsId, userId);
        documentChunkRepository.deleteAllByFaqDocumentId(documentId);
        faqDocumentRepository.deleteById(documentId);
        return ResponseEntity.noContent().build();
    }

    public record IngestRequest(@NotNull UUID attachmentId) {}
}
