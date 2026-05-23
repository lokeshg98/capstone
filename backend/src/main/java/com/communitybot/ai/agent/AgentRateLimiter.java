package com.communitybot.ai.agent;

import com.communitybot.ai.config.AgentProperties;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Limits total agent runs per workspace per day (Ask panel + @bot), separate from legacy RAG-only limit.
 */
@Component
@RequiredArgsConstructor
public class AgentRateLimiter {

    private final StringRedisTemplate redis;
    private final AgentProperties     props;

    public void consume(UUID workspaceId) {
        String key   = "agent:daily:" + workspaceId + ":" + LocalDate.now();
        Long   count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofHours(25));
        }
        if (count != null && count > props.getDailyWorkspaceLimit()) {
            throw new AppException(ErrorCode.RAG_RATE_LIMIT_EXCEEDED);
        }
    }
}
