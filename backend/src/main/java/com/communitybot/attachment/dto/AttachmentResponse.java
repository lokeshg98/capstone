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
        String kind,       // PDF | DOCX | JPEG | TXT | MD
        long   sizeBytes,
        String scanStatus  // PENDING | CLEAN | INFECTED | ERROR
) {
    public static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(
                a.getId(),
                a.getFilename(),
                a.getMimeType(),
                a.getKind().name(),
                a.getSizeBytes(),
                a.getScanStatus().name()
        );
    }
}
