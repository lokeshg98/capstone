package com.communitybot.moderation.service;

import com.communitybot.ai.usage.LlmUsageCategory;
import com.communitybot.ai.usage.LlmUsageService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * Calls OpenAI's Moderation API ({@code POST /v1/moderations}) to detect
 * harassment, hate, violence, sexual content, and self-harm.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAiModerationService {

    private final @Qualifier("openAiRestClient") RestClient restClient;
    private final com.communitybot.moderation.config.ModerationProperties moderationProperties;
    private final LlmUsageService  llmUsageService;

    public ModerationApiResult moderate(String text, UUID attributingUserId) {
        if (text == null || text.isBlank()) {
            return ModerationApiResult.safe();
        }

        try {
            var request = new ModerationRequest(moderationProperties.getOpenaiModerationModel(), text);
            ModerationResponse response = restClient.post()
                    .uri("/moderations")
                    .body(request)
                    .retrieve()
                    .body(ModerationResponse.class);

            if (response == null || response.results() == null || response.results().isEmpty()) {
                log.warn("Empty moderation API response");
                return ModerationApiResult.safe();
            }

            ModerationResult result = response.results().get(0);
            if (attributingUserId != null) {
                llmUsageService.record(
                        attributingUserId,
                        LlmUsageCategory.MODERATION,
                        moderationProperties.getOpenaiModerationModel(),
                        estimateTokens(text),
                        0
                );
            }

            if (!result.flagged()) {
                return ModerationApiResult.safe();
            }

            String topCategory = pickTopCategory(result.categories(), result.categoryScores());
            double score = topScore(result.categoryScores(), topCategory);
            return new ModerationApiResult(
                    true,
                    topCategory,
                    score,
                    "OpenAI Moderation API flagged this message for " + topCategory.replace('_', ' ')
            );
        } catch (Exception e) {
            log.warn("OpenAI Moderation API error: {}", e.getMessage());
            return ModerationApiResult.safe();
        }
    }

    private static String pickTopCategory(Map<String, Boolean> categories, Map<String, Double> scores) {
        if (categories != null) {
            for (Map.Entry<String, Boolean> e : categories.entrySet()) {
                if (Boolean.TRUE.equals(e.getValue())) {
                    return e.getKey().toUpperCase();
                }
            }
        }
        if (scores != null && !scores.isEmpty()) {
            return scores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> e.getKey().toUpperCase())
                    .orElse("POLICY_VIOLATION");
        }
        return "POLICY_VIOLATION";
    }

    private static double topScore(Map<String, Double> scores, String category) {
        if (scores == null) return 1.0;
        return scores.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(category))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(1.0);
    }

    private static int estimateTokens(String text) {
        return Math.max(1, text.length() / 4);
    }

    public record ModerationApiResult(
            boolean flagged,
            String category,
            double score,
            String explanation
    ) {
        public static ModerationApiResult safe() {
            return new ModerationApiResult(false, "SAFE", 0.0, "");
        }
    }

    private record ModerationRequest(String model, String input) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ModerationResponse(java.util.List<ModerationResult> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ModerationResult(
            boolean flagged,
            Map<String, Boolean> categories,
            @JsonProperty("category_scores") Map<String, Double> categoryScores
    ) {}
}
