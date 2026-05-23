package com.communitybot.attachment.service;

import com.communitybot.attachment.domain.Attachment;
import com.communitybot.attachment.domain.AttachmentKind;
import com.communitybot.attachment.domain.ScanStatus;
import com.communitybot.attachment.dto.AttachmentResponse;
import com.communitybot.attachment.repository.AttachmentRepository;
import com.communitybot.auth.domain.User;
import com.communitybot.auth.service.UserService;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AttachmentService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".docx", ".jpg", ".jpeg", ".md", ".txt"
    );

    private final AttachmentRepository attachmentRepository;
    private final StorageService       storageService;
    private final ClamAvService        clamAvService;
    private final MimeSnifferService   mimeSnifferService;
    private final UserService          userService;

    @Value("${app.upload.max-size-bytes:10485760}")
    private long maxSizeBytes;

    @Value("${app.clamav.enabled:true}")
    private boolean clamAvEnabled;

    /**
     * Full upload pipeline:
     * 1. Size check
     * 2. Extension allow-list (first defence)
     * 3. Magic-byte MIME sniff via Apache Tika (second defence)
     * 4. Store in MinIO
     * 5. ClamAV virus scan on stored object (third defence, optional in dev)
     * 6. Persist metadata in DB
     */
    @Transactional
    public AttachmentResponse upload(MultipartFile file, UUID uploaderId) {
        if (file.getSize() > maxSizeBytes) {
            throw new AppException(ErrorCode.ATTACHMENT_TOO_LARGE);
        }

        String originalName = sanitizeFilename(file.getOriginalFilename());
        validateExtension(originalName);

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new AppException(ErrorCode.ATTACHMENT_UPLOAD_FAILED);
        }

        if (content.length > maxSizeBytes) {
            throw new AppException(ErrorCode.ATTACHMENT_TOO_LARGE);
        }

        int headerLen = Math.min(content.length, 4096);
        String detectedMime = mimeSnifferService.detectMime(
                java.util.Arrays.copyOf(content, headerLen)
        );

        if (!mimeSnifferService.isAllowed(detectedMime)) {
            throw new AppException(ErrorCode.ATTACHMENT_TYPE_NOT_ALLOWED,
                    "Detected MIME type: " + detectedMime);
        }
        AttachmentKind kind = mimeSnifferService.toKind(detectedMime, originalName);
        validateExtensionMatchesKind(originalName, kind);

        String objectKey = uploaderId + "/" + UUID.randomUUID() + "/" + originalName;
        try (InputStream is = new ByteArrayInputStream(content)) {
            storageService.upload(objectKey, is, content.length, detectedMime);
        } catch (IOException e) {
            throw new AppException(ErrorCode.ATTACHMENT_UPLOAD_FAILED);
        }

        ScanStatus scanStatus = scanStoredObject(objectKey, originalName);

        User uploader = userService.getOrThrow(uploaderId);
        Attachment saved;
        try {
            saved = attachmentRepository.save(
                    Attachment.builder()
                            .uploadedBy(uploader)
                            .filename(originalName)
                            .mimeType(detectedMime)
                            .sizeBytes((long) content.length)
                            .kind(kind)
                            .storageKey(objectKey)
                            .scanStatus(scanStatus)
                            .build()
            );
        } catch (Exception e) {
            storageService.delete(objectKey);
            throw new AppException(ErrorCode.ATTACHMENT_UPLOAD_FAILED);
        }

        return AttachmentResponse.from(saved);
    }

    /**
     * Streams the attachment content back to the caller (used by the download endpoint).
     * Returns the raw InputStream — caller must close it.
     */
    public Attachment getOrThrow(UUID attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ATTACHMENT_NOT_FOUND));
    }

    public void requireClean(Attachment attachment) {
        if (attachment.getScanStatus() != ScanStatus.CLEAN) {
            throw new AppException(ErrorCode.ATTACHMENT_SCAN_FAILED,
                    "This file is not available yet — virus scan did not complete successfully");
        }
    }

    public InputStream openStream(Attachment attachment) {
        requireClean(attachment);
        return storageService.download(attachment.getStorageKey());
    }

    public long maxSizeBytes() {
        return maxSizeBytes;
    }

    // -------------------------------------------------------------------------

    private ScanStatus scanStoredObject(String objectKey, String filename) {
        if (!clamAvEnabled) {
            log.debug("ClamAV disabled — marking '{}' as clean without scan", filename);
            return ScanStatus.CLEAN;
        }

        try (InputStream is = storageService.download(objectKey)) {
            ClamAvService.ScanResult result = clamAvService.scan(is);
            log.info("AV scan: file='{}' result='{}'", filename, result.rawResponse());
            if (!result.clean()) {
                storageService.delete(objectKey);
                throw new AppException(ErrorCode.ATTACHMENT_INFECTED);
            }
            return ScanStatus.CLEAN;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("ClamAV unreachable for '{}': {}", filename, e.getMessage());
            storageService.delete(objectKey);
            throw new AppException(ErrorCode.ATTACHMENT_SCAN_FAILED);
        }
    }

    private String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) return "upload";
        return original.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    private void validateExtension(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        boolean allowed = ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
        if (!allowed) {
            throw new AppException(ErrorCode.ATTACHMENT_TYPE_NOT_ALLOWED);
        }
    }

    private void validateExtensionMatchesKind(String filename, AttachmentKind kind) {
        String lower = filename.toLowerCase(Locale.ROOT);
        boolean ok = switch (kind) {
            case PDF -> lower.endsWith(".pdf");
            case DOCX -> lower.endsWith(".docx");
            case JPEG -> lower.endsWith(".jpg") || lower.endsWith(".jpeg");
            case TXT -> lower.endsWith(".txt");
            case MD -> lower.endsWith(".md");
        };
        if (!ok) {
            throw new AppException(ErrorCode.ATTACHMENT_TYPE_NOT_ALLOWED,
                    "File extension does not match detected content type");
        }
    }
}
