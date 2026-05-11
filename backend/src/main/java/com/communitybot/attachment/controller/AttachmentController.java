package com.communitybot.attachment.controller;

import com.communitybot.attachment.domain.Attachment;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

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
     * PDFs are served inline so the browser can render them directly.
     * DOCX files are served as attachments (download).
     */
    @GetMapping("/{attachmentId}/content")
    public ResponseEntity<StreamingResponseBody> content(
            @PathVariable UUID attachmentId,
            @CurrentUser UUID userId
    ) {
        Attachment attachment = attachmentService.getOrThrow(attachmentId);

        MediaType mediaType = MediaType.parseMediaType(attachment.getMimeType());

        ContentDisposition disposition = attachment.getKind().name().equals("PDF")
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
}
