package com.communitybot.moderation.service;

import com.communitybot.moderation.config.ModerationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Two-layer moderation pipeline:
 * <ol>
 *   <li>OpenAI Moderation API — fast policy check for harassment, hate, violence, etc.</li>
 *   <li>Custom community guidelines — LLM judge using workspace rules + default {@code community-guidelines.md}</li>
 * </ol>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContentClassifierService {

    private final OpenAiModerationService    openAiModerationService;
    private final GuidelinesClassifierService guidelinesClassifier;
    private final CommunityGuidelinesService guidelinesService;
    private final ModerationProperties       moderationProperties;

    public ClassificationResult classify(String messageBody, UUID workspaceId, UUID authorId) {
        if (messageBody == null || messageBody.isBlank()) {
            return ClassificationResult.ofSafe();
        }

        if (moderationProperties.isOpenaiModerationEnabled()) {
            OpenAiModerationService.ModerationApiResult api =
                    openAiModerationService.moderate(messageBody, authorId);
            if (api.flagged()) {
                log.debug("OpenAI Moderation flagged: category={} score={}", api.category(), api.score());
                return new ClassificationResult(
                        false,
                        "OPENAI_" + api.category(),
                        Math.max(api.score(), 0.95),
                        api.explanation(),
                        ModerationSource.OPENAI
                );
            }
        }

        if (moderationProperties.isGuidelinesCheckEnabled()) {
            String guidelines = guidelinesService.resolveForWorkspace(workspaceId);
            ClassificationResult guidelinesResult =
                    guidelinesClassifier.classify(messageBody, guidelines, authorId);
            if (!guidelinesResult.safe()) {
                return new ClassificationResult(
                        false,
                        guidelinesResult.flagReason(),
                        guidelinesResult.confidence(),
                        guidelinesResult.explanation(),
                        ModerationSource.GUIDELINES
                );
            }
        }

        return ClassificationResult.ofSafe();
    }

    public enum ModerationSource {
        OPENAI,
        GUIDELINES
    }

    public record ClassificationResult(
            boolean safe,
            String flagReason,
            double confidence,
            String explanation,
            ModerationSource source
    ) {
        public ClassificationResult(boolean safe, String flagReason, double confidence, String explanation) {
            this(safe, flagReason, confidence, explanation, null);
        }

        public static ClassificationResult ofSafe() {
            return new ClassificationResult(true, "SAFE", 1.0, "", null);
        }
    }
}
