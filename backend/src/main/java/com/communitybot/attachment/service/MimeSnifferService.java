package com.communitybot.attachment.service;

import com.communitybot.attachment.domain.AttachmentKind;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

/**
 * Detects file MIME type from magic bytes using Apache Tika.
 * Trusts the file content, not the client-supplied Content-Type header.
 */
@Service
public class MimeSnifferService {

    private static final Tika TIKA = new Tika();

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "text/plain",
            "text/markdown"
    );

    /**
     * Reads the first bytes of the file (typically 4 KB is enough for all magic-byte patterns)
     * and returns the detected MIME type string.
     */
    public String detectMime(byte[] headerBytes) {
        return TIKA.detect(headerBytes);
    }

    public boolean isAllowed(String mimeType) {
        return ALLOWED_MIME_TYPES.contains(mimeType);
    }

    /**
     * Maps a detected MIME type (and filename for plain-text disambiguation) to {@link AttachmentKind}.
     */
    public AttachmentKind toKind(String mimeType, String filename) {
        String lowerName = filename.toLowerCase(Locale.ROOT);

        if ("text/plain".equals(mimeType) || "text/markdown".equals(mimeType)) {
            if (lowerName.endsWith(".md")) {
                return AttachmentKind.MD;
            }
            if (lowerName.endsWith(".txt")) {
                return AttachmentKind.TXT;
            }
            throw new AppException(ErrorCode.ATTACHMENT_TYPE_NOT_ALLOWED,
                    "Plain-text uploads must use a .txt or .md extension");
        }

        return switch (mimeType) {
            case "application/pdf" -> AttachmentKind.PDF;
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> AttachmentKind.DOCX;
            case "image/jpeg" -> AttachmentKind.JPEG;
            default -> throw new AppException(ErrorCode.ATTACHMENT_TYPE_NOT_ALLOWED);
        };
    }
}
