package com.communitybot.realtime.controller;

import com.communitybot.auth.domain.User;
import com.communitybot.auth.service.UserService;
import com.communitybot.message.dto.MessageResponse;
import com.communitybot.message.service.MessageService;
import com.communitybot.realtime.dto.WsOutboundEvent;
import com.communitybot.realtime.dto.WsSendMessagePayload;
import com.communitybot.realtime.service.PresenceService;
import com.communitybot.realtime.service.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * Handles inbound STOMP messages.
 *
 * Clients send to:
 * <ul>
 *   <li>{@code /app/channels/{channelId}/send}   — deliver a new message</li>
 *   <li>{@code /app/channels/{channelId}/typing} — broadcast a typing indicator</li>
 * </ul>
 *
 * Both handlers publish their result to Redis, which the subscriber
 * ({@link com.communitybot.realtime.service.RedisChannelSubscriber}) forwards to
 * {@code /topic/channels/{channelId}}.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final MessageService    messageService;
    private final UserService       userService;
    private final RealtimePublisher publisher;
    private final PresenceService   presenceService;

    @MessageMapping("/channels/{channelId}/send")
    public void send(
            @DestinationVariable UUID channelId,
            @Payload WsSendMessagePayload payload,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        UUID userId = resolveUserId(headerAccessor);
        MessageResponse msg = messageService.send(channelId, userId, payload.body(), payload.threadRootId(), payload.attachmentId());
        publisher.publishToChannel(channelId, WsOutboundEvent.messageCreated(msg));
        log.debug("WS message sent: channelId={} msgId={}", channelId, msg.id());
    }

    @MessageMapping("/channels/{channelId}/typing")
    public void typing(
            @DestinationVariable UUID channelId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        UUID userId = resolveUserId(headerAccessor);
        User user   = userService.getOrThrow(userId);
        publisher.publishToChannel(channelId, WsOutboundEvent.typing(userId, user.getDisplayName()));
    }

    @MessageMapping("/presence/{wsId}/join")
    public void presenceJoin(
            @DestinationVariable UUID wsId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        UUID userId = resolveUserId(headerAccessor);
        presenceService.userJoinedWorkspace(wsId, userId);
        log.debug("Presence join: userId={} wsId={}", userId, wsId);
    }

    @MessageMapping("/presence/{wsId}/leave")
    public void presenceLeave(
            @DestinationVariable UUID wsId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        UUID userId = resolveUserId(headerAccessor);
        presenceService.userLeftWorkspace(wsId, userId);
        log.debug("Presence leave: userId={} wsId={}", userId, wsId);
    }

    // -------------------------------------------------------------------------

    private UUID resolveUserId(SimpMessageHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new IllegalStateException("Unauthenticated WebSocket message — should not reach handler");
        }
        // Principal name is the userId string set in WebSocketAuthInterceptor
        return UUID.fromString(principal.getName());
    }
}
