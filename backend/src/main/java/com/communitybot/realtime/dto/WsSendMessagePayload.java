package com.communitybot.realtime.dto;

import java.util.UUID;

/**
 * Inbound STOMP payload when a client sends a message via WebSocket.
 * Mirrors {@link com.communitybot.message.dto.SendMessageRequest} but kept separate
 * because WS and HTTP request shapes may diverge over time.
 */
public record WsSendMessagePayload(
        String body,
        UUID   threadRootId,  // null → top-level message
        UUID   attachmentId   // null → text-only message
) {
}
