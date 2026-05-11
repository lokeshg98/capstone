package com.communitybot.realtime.service;

import com.communitybot.realtime.dto.WsOutboundEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Serialises outbound events to JSON and publishes them to a Redis channel.
 *
 * The Redis key pattern is {@code channel:{channelId}}, which the pattern subscriber
 * picks up and forwards to the STOMP topic {@code /topic/channels/{channelId}}.
 * This fan-out approach keeps the chat delivery path multi-instance safe.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RealtimePublisher {

    private static final String KEY_PREFIX = "channel:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;

    public void publishToChannel(UUID channelId, WsOutboundEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(KEY_PREFIX + channelId, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise WsOutboundEvent for channel {}", channelId, e);
        }
    }
}
