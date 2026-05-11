package com.communitybot.shared.config;

import com.communitybot.realtime.service.RedisChannelSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Wires the Redis message-listener container that subscribes to {@code channel:*}
 * and forwards events to STOMP via {@link RedisChannelSubscriber}.
 *
 * {@link org.springframework.data.redis.core.StringRedisTemplate} is auto-configured
 * by Spring Boot — no need to define it here.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisChannelSubscriber subscriber
    ) {
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // Pattern subscription — picks up all channel:* keys without listing individual channels
        container.addMessageListener(subscriber, new PatternTopic("channel:*"));
        return container;
    }
}
