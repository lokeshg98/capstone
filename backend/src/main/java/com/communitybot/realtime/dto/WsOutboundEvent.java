package com.communitybot.realtime.dto;

import com.communitybot.message.dto.MessageResponse;

import java.util.UUID;

/**
 * Envelope pushed to STOMP topic {@code /topic/channels/{channelId}}.
 *
 * The {@code eventType} string tells the frontend how to cast {@code data}:
 * <ul>
 *   <li>{@code MESSAGE_CREATED}  — data is a {@link MessageResponse}</li>
 *   <li>{@code MESSAGE_UPDATED}  — data is a {@link MessageResponse}</li>
 *   <li>{@code REACTION_UPDATED} — data is a {@link MessageResponse}</li>
 *   <li>{@code TYPING}           — data is a {@link TypingPayload}</li>
 * </ul>
 *
 * Using {@code Object} keeps Jackson serialisation simple — it serialises the
 * concrete runtime type into the JSON.  The frontend receives it as a plain
 * JSON object inside the {@code data} field.
 */
public record WsOutboundEvent(String eventType, Object data) {

    public static WsOutboundEvent messageCreated(MessageResponse msg) {
        return new WsOutboundEvent("MESSAGE_CREATED", msg);
    }

    public static WsOutboundEvent messageUpdated(MessageResponse msg) {
        return new WsOutboundEvent("MESSAGE_UPDATED", msg);
    }

    public static WsOutboundEvent reactionUpdated(MessageResponse msg) {
        return new WsOutboundEvent("REACTION_UPDATED", msg);
    }

    public static WsOutboundEvent typing(UUID userId, String displayName) {
        return new WsOutboundEvent("TYPING", new TypingPayload(userId, displayName));
    }

    public record TypingPayload(UUID userId, String displayName) {}
}
