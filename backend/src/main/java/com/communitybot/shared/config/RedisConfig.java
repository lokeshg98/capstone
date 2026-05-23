package com.communitybot.shared.config;

import com.communitybot.realtime.service.RedisChannelSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Wires the Redis message-listener container that subscribes to
 * {@code channel:*} and {@code workspace:*} and forwards events to STOMP.
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
        container.addMessageListener(subscriber, new PatternTopic("channel:*"));
        container.addMessageListener(subscriber, new PatternTopic("workspace:*"));
        return container;
    }
}
