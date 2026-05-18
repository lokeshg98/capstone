package com.communitybot.ai.usage.dto;

import java.math.BigDecimal;

/**
 * Aggregated LLM usage for the signed-in user vs the whole deployment ("project").
 */
public record LlmUsageSummaryResponse(
        long userInputTokens,
        long userOutputTokens,
        long projectInputTokens,
        long projectOutputTokens,
        BigDecimal userTotalCostUsd,
        BigDecimal projectTotalCostUsd
) {}
