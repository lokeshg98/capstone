package com.communitybot.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

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
    private int    maxTokens      = 2048;
    private double temperature    = 0.7;
    /** Max RAG calls per workspace per calendar day (cost guardrail). */
    private int    dailyRagLimit  = 100;

    /**
     * Per-model rates in USD per 1M tokens (OpenAI list prices; override as needed).
     */
    private Map<String, ModelRate> modelRates = new LinkedHashMap<>();

    @Data
    public static class ModelRate {
        private BigDecimal inputPerMillion  = BigDecimal.ZERO;
        private BigDecimal outputPerMillion = BigDecimal.ZERO;
    }
}
