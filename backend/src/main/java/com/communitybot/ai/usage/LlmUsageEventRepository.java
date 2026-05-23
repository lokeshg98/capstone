package com.communitybot.ai.usage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LlmUsageEventRepository extends JpaRepository<LlmUsageEvent, UUID> {

    @Query(value = """
            SELECT COALESCE(SUM(input_tokens), 0),
                   COALESCE(SUM(output_tokens), 0),
                   COALESCE(SUM(cost_usd), 0)
            FROM llm_usage_events
            WHERE user_id = :userId
            """, nativeQuery = true)
    Optional<Object[]> sumForUser(@Param("userId") UUID userId);

    @Query(value = """
            SELECT COALESCE(SUM(input_tokens), 0),
                   COALESCE(SUM(output_tokens), 0),
                   COALESCE(SUM(cost_usd), 0)
            FROM llm_usage_events
            """, nativeQuery = true)
    Optional<Object[]> sumProject();
}
