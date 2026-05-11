package com.communitybot.attachment.dto;

import com.communitybot.attachment.domain.Attachment;

import java.util.UUID;

/**
 * Returned after a successful upload.
 * Intentionally omits the internal storageKey.
 */
public record AttachmentResponse(
        UUID   id,
        String filename,
        String mimeType,
        String kind,       // "PDF" | "DOCX"
        long   sizeBytes
) {
    public static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(
                a.getId(),
                a.getFilename(),
                a.getMimeType(),
                a.getKind().name(),
                a.getSizeBytes()
        );
    }
}
