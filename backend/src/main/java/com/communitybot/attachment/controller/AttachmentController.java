package com.communitybot.attachment.controller;

import com.communitybot.attachment.domain.Attachment;
import com.communitybot.attachment.domain.AttachmentKind;
import com.communitybot.attachment.dto.AttachmentLimitsResponse;
import com.communitybot.attachment.dto.AttachmentResponse;
import com.communitybot.attachment.service.AttachmentService;
import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/limits")
    public ResponseEntity<ApiResponse<AttachmentLimitsResponse>> limits() {
        return ResponseEntity.ok(ApiResponse.ok(new AttachmentLimitsResponse(
                attachmentService.maxSizeBytes(),
                List.of(".jpg", ".jpeg", ".pdf", ".docx", ".md", ".txt")
        )));
    }

    /**
     * Uploads a file through the full validation + scan pipeline.
     * Returns attachment metadata; the client then includes the returned {@code id}
     * in a subsequent message-send request.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AttachmentResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @CurrentUser UUID userId
    ) {
        AttachmentResponse response = attachmentService.upload(file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    /**
     * Streams the file content through the backend (avoids MinIO CORS concerns).
     * Images and PDFs are served inline; other types download by default.
     */
    @GetMapping("/{attachmentId}/content")
    public ResponseEntity<StreamingResponseBody> content(
            @PathVariable UUID attachmentId,
            @CurrentUser UUID userId
    ) {
        Attachment attachment = attachmentService.getOrThrow(attachmentId);
        attachmentService.requireClean(attachment);

        MediaType mediaType = MediaType.parseMediaType(attachment.getMimeType());

        ContentDisposition disposition = isInlineKind(attachment.getKind())
                ? ContentDisposition.inline().filename(attachment.getFilename()).build()
                : ContentDisposition.attachment().filename(attachment.getFilename()).build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(disposition);
        headers.setContentLength(attachment.getSizeBytes());

        StreamingResponseBody body = outputStream -> {
            try (var inputStream = attachmentService.openStream(attachment)) {
                inputStream.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .headers(headers)
                .body(body);
    }

    private static boolean isInlineKind(AttachmentKind kind) {
        return kind == AttachmentKind.PDF
                || kind == AttachmentKind.JPEG
                || kind == AttachmentKind.TXT
                || kind == AttachmentKind.MD;
    }
}
