package com.communitybot.moderation.dto;

import com.communitybot.moderation.domain.FlagStatus;
import com.communitybot.moderation.domain.ModerationFlag;

import java.time.Instant;
import java.util.UUID;

public record ModerationFlagResponse(
        UUID       flagId,
        UUID       messageId,
        String     messageBody,
        AuthorInfo messageAuthor,
        String     llmReason,
        String     llmExplanation,
        double     llmConfidence,
        FlagStatus status,
        Instant    flaggedAt,
        UUID       reviewedBy,
        Instant    reviewedAt
) {
    public record AuthorInfo(UUID id, String displayName, String avatarUrl) {}

    public static ModerationFlagResponse from(ModerationFlag flag) {
        var author = flag.getMessage().getAuthor();
        return new ModerationFlagResponse(
                flag.getId(),
                flag.getMessage().getId(),
                flag.getMessage().getBody(),
                new AuthorInfo(author.getId(), author.getDisplayName(), author.getAvatarUrl()),
                flag.getLlmReason(),
                flag.getLlmExplanation(),
                flag.getLlmConfidence() != null ? flag.getLlmConfidence() : 0.0,
                flag.getStatus(),
                flag.getCreatedAt(),
                flag.getReviewedBy(),
                flag.getReviewedAt()
        );
    }
}
