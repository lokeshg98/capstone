package com.communitybot.realtime.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Listens on Redis pattern {@code channel:*} and forwards the raw JSON payload
 * to the corresponding STOMP topic.
 *
 * Sending a pre-serialised JSON string to {@link SimpMessagingTemplate} uses
 * Spring's {@code StringMessageConverter} which passes it through verbatim —
 * no double serialisation.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisChannelSubscriber implements MessageListener {

    private static final String KEY_PREFIX  = "channel:";
    private static final String STOMP_PREFIX = "/topic/channels/";

    private final SimpMessagingTemplate stompTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channelKey = new String(message.getChannel(), StandardCharsets.UTF_8);
        String eventJson  = new String(message.getBody(),   StandardCharsets.UTF_8);

        if (!channelKey.startsWith(KEY_PREFIX)) {
            log.warn("Received message on unexpected Redis key: {}", channelKey);
            return;
        }

        String channelId = channelKey.substring(KEY_PREFIX.length());
        log.debug("Redis → STOMP: channelId={} payload={}B", channelId, eventJson.length());

        stompTemplate.convertAndSend(STOMP_PREFIX + channelId, eventJson);
    }
}
