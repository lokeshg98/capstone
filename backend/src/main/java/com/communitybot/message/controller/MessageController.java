package com.communitybot.message.controller;

import com.communitybot.message.dto.AddReactionRequest;
import com.communitybot.message.dto.MessageResponse;
import com.communitybot.message.dto.SendMessageRequest;
import com.communitybot.message.service.MessageService;
import com.communitybot.realtime.dto.WsOutboundEvent;
import com.communitybot.realtime.service.RealtimePublisher;
import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.dto.PageResponse;
import com.communitybot.shared.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * HTTP endpoints for messages — the WebSocket path (ChatWebSocketController)
 * handles real-time sends; this controller provides history loading,
 * REST-fallback sends, edits, deletes, and reactions.
 */
@RestController
@RequestMapping("/api/channels/{channelId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService   messageService;
    private final RealtimePublisher realtimePublisher;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MessageResponse>>> list(
            @PathVariable UUID channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                messageService.loadPage(channelId, userId, page, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> send(
            @PathVariable UUID channelId,
            @Valid @RequestBody SendMessageRequest req,
            @CurrentUser UUID userId
    ) {
        MessageResponse msg = messageService.send(channelId, userId, req.body(), req.threadRootId(), req.attachmentId());
        realtimePublisher.publishToChannel(channelId, WsOutboundEvent.messageCreated(msg));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(msg));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID channelId,
            @PathVariable UUID messageId,
            @CurrentUser UUID userId
    ) {
        messageService.delete(channelId, messageId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<ApiResponse<MessageResponse>> addReaction(
            @PathVariable UUID channelId,
            @PathVariable UUID messageId,
            @Valid @RequestBody AddReactionRequest req,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                messageService.addReaction(messageId, req.emoji(), userId)));
    }

    @DeleteMapping("/{messageId}/reactions/{emoji}")
    public ResponseEntity<ApiResponse<MessageResponse>> removeReaction(
            @PathVariable UUID channelId,
            @PathVariable UUID messageId,
            @PathVariable String emoji,
            @CurrentUser UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                messageService.removeReaction(messageId, emoji, userId)));
    }
}
