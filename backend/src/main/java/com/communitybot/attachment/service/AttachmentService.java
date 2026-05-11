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

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AttachmentService {

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
     * 4. ClamAV virus scan (third defence, optional in dev)
     * 5. Store in MinIO
     * 6. Persist metadata in DB
     */
    @Transactional
    public AttachmentResponse upload(MultipartFile file, UUID uploaderId) {
        // 1. Size check
        if (file.getSize() > maxSizeBytes) {
            throw new AppException(ErrorCode.ATTACHMENT_TOO_LARGE);
        }

        // 2. Extension check (cheap first gate)
        String originalName = sanitizeFilename(file.getOriginalFilename());
        validateExtension(originalName);

        // 3. MIME sniff (read only the header bytes — don't hold the full file in memory)
        String detectedMime;
        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(4096);
            detectedMime = mimeSnifferService.detectMime(header);
        } catch (IOException e) {
            throw new AppException(ErrorCode.ATTACHMENT_UPLOAD_FAILED);
        }

        if (!mimeSnifferService.isAllowed(detectedMime)) {
            throw new AppException(ErrorCode.ATTACHMENT_TYPE_NOT_ALLOWED,
                    "Detected MIME type: " + detectedMime);
        }
        AttachmentKind kind = mimeSnifferService.toKind(detectedMime);

        // 4. ClamAV scan
        if (clamAvEnabled) {
            try (InputStream is = file.getInputStream()) {
                ClamAvService.ScanResult result = clamAvService.scan(is);
                log.info("AV scan: file='{}' result='{}'", originalName, result.rawResponse());
                if (!result.clean()) {
                    throw new AppException(ErrorCode.ATTACHMENT_INFECTED);
                }
            } catch (AppException e) {
                throw e;
            } catch (Exception e) {
                log.error("ClamAV unreachable: {}", e.getMessage());
                throw new AppException(ErrorCode.ATTACHMENT_SCAN_FAILED);
            }
        }

        // 5. Upload to MinIO; if DB save later fails we clean up the orphan
        String objectKey = uploaderId + "/" + UUID.randomUUID() + "/" + originalName;
        try (InputStream is = file.getInputStream()) {
            storageService.upload(objectKey, is, file.getSize(), detectedMime);
        } catch (IOException e) {
            throw new AppException(ErrorCode.ATTACHMENT_UPLOAD_FAILED);
        }

        // 6. Persist
        User uploader = userService.getOrThrow(uploaderId);
        Attachment saved;
        try {
            saved = attachmentRepository.save(
                    Attachment.builder()
                            .uploadedBy(uploader)
                            .filename(originalName)
                            .mimeType(detectedMime)
                            .sizeBytes(file.getSize())
                            .kind(kind)
                            .storageKey(objectKey)
                            .scanStatus(ScanStatus.CLEAN)
                            .build()
            );
        } catch (Exception e) {
            // Best-effort cleanup: remove the orphaned MinIO object
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

    public InputStream openStream(Attachment attachment) {
        return storageService.download(attachment.getStorageKey());
    }

    // -------------------------------------------------------------------------

    private String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) return "upload";
        // Strip path separators and other dangerous characters
        return original.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    private void validateExtension(String filename) {
        String lower = filename.toLowerCase();
        if (!lower.endsWith(".pdf") && !lower.endsWith(".docx")) {
            throw new AppException(ErrorCode.ATTACHMENT_TYPE_NOT_ALLOWED,
                    "Only .pdf and .docx extensions are accepted");
        }
    }
}
