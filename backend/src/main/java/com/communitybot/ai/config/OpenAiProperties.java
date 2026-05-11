package com.communitybot.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from {@code app.openai.*} in application.yml.
 * Auto-registered by {@code @ConfigurationPropertiesScan} on the main class.
 */
@Data
@ConfigurationProperties(prefix = "app.openai")
public class OpenAiProperties {

    private String apiKey;
    private String embeddingModel = "text-embedding-3-small";
    private String chatModel      = "gpt-4o-mini";
    private int    maxTokens      = 512;
    private double temperature    = 0.7;
    /** Max RAG calls per workspace per calendar day (cost guardrail). */
    private int    dailyRagLimit  = 100;
}
