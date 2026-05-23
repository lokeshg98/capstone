package com.communitybot.realtime.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Listens on Redis patterns {@code channel:*} and {@code workspace:*} and
 * forwards the raw JSON payload to the corresponding STOMP topic.
 *
 * <ul>
 *   <li>{@code channel:*}   → {@code /topic/channels/{id}}</li>
 *   <li>{@code workspace:*} → {@code /topic/workspaces/{id}/presence}</li>
 * </ul>
 *
 * Sending a pre-serialised JSON string to {@link SimpMessagingTemplate} uses
 * Spring's {@code StringMessageConverter} which passes it through verbatim —
 * no double serialisation.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisChannelSubscriber implements MessageListener {

    private static final String CHANNEL_KEY_PREFIX   = "channel:";
    private static final String WORKSPACE_KEY_PREFIX = "workspace:";
    private static final String STOMP_CHANNEL_PREFIX = "/topic/channels/";
    private static final String STOMP_WS_PRESENCE    = "/topic/workspaces/%s/presence";

    private final SimpMessagingTemplate stompTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channelKey = new String(message.getChannel(), StandardCharsets.UTF_8);
        String eventJson  = new String(message.getBody(),   StandardCharsets.UTF_8);

        if (channelKey.startsWith(CHANNEL_KEY_PREFIX)) {
            String channelId = channelKey.substring(CHANNEL_KEY_PREFIX.length());
            stompTemplate.convertAndSend(STOMP_CHANNEL_PREFIX + channelId, eventJson);
        } else if (channelKey.startsWith(WORKSPACE_KEY_PREFIX)) {
            String wsId = channelKey.substring(WORKSPACE_KEY_PREFIX.length());
            stompTemplate.convertAndSend(STOMP_WS_PRESENCE.formatted(wsId), eventJson);
        } else {
            log.warn("Received message on unexpected Redis channel: {}", channelKey);
        }
    }
}
