package com.communitybot.attachment.service;

import com.communitybot.attachment.domain.AttachmentKind;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

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
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
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

    public AttachmentKind toKind(String mimeType) {
        return switch (mimeType) {
            case "application/pdf" -> AttachmentKind.PDF;
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> AttachmentKind.DOCX;
            default -> throw new AppException(ErrorCode.ATTACHMENT_TYPE_NOT_ALLOWED);
        };
    }
}
