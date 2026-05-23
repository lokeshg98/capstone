package com.communitybot.ai.usage.dto;

/**
 * Aggregated LLM usage for the signed-in user vs the whole deployment ("project").
 * Costs are JSON numbers (USD estimates from configured model rates).
 */
public record LlmUsageSummaryResponse(
        long   userInputTokens,
        long   userOutputTokens,
        long   projectInputTokens,
        long   projectOutputTokens,
        double userTotalCostUsd,
        double projectTotalCostUsd
) {
    public static LlmUsageSummaryResponse empty() {
        return new LlmUsageSummaryResponse(0, 0, 0, 0, 0, 0);
    }
}
