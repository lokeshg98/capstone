package com.communitybot.document.dto;

import com.communitybot.document.domain.FaqDocument;

import java.time.Instant;
import java.util.UUID;

public record FaqDocumentResponse(
        UUID    id,
        UUID    workspaceId,
        String  title,
        int     chunkCount,
        Instant ingestedAt
) {
    public static FaqDocumentResponse from(FaqDocument doc) {
        return new FaqDocumentResponse(
                doc.getId(),
                doc.getWorkspace().getId(),
                doc.getTitle(),
                doc.getChunkCount() != null ? doc.getChunkCount() : 0,
                doc.getCreatedAt()
        );
    }
}
