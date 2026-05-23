package com.communitybot.realtime.service;

import com.communitybot.realtime.dto.WsOutboundEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Serialises outbound events to JSON and publishes them to Redis.
 *
 * <ul>
 *   <li>{@code channel:{channelId}} → forwarded by subscriber to {@code /topic/channels/{channelId}}</li>
 *   <li>{@code workspace:{wsId}}    → forwarded by subscriber to {@code /topic/workspaces/{wsId}/presence}</li>
 * </ul>
 *
 * This fan-out approach keeps all delivery paths multi-instance safe.
 *
 * <p>All errors are caught and logged — a Redis failure must never block or
 * fail the caller's response (HTTP or WebSocket).</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RealtimePublisher {

    private static final String CHANNEL_KEY_PREFIX   = "channel:";
    private static final String WORKSPACE_KEY_PREFIX = "workspace:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;

    public void publishToChannel(UUID channelId, WsOutboundEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(CHANNEL_KEY_PREFIX + channelId, json);
        } catch (Exception e) {
            log.error("Failed to publish event to channel {}: {}", channelId, e.getMessage(), e);
        }
    }

    /** Publishes a presence update via Redis to the workspace topic. */
    public void publishPresenceUpdate(UUID workspaceId, UUID userId, boolean online) {
        try {
            WsOutboundEvent event = WsOutboundEvent.presenceUpdated(userId, online);
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(WORKSPACE_KEY_PREFIX + workspaceId, json);
        } catch (Exception e) {
            log.error("Failed to publish presence update for userId={} ws={}: {}", userId, workspaceId, e.getMessage());
        }
    }
}
