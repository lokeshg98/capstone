package com.communitybot.moderation.domain;

import com.communitybot.message.domain.Message;
import com.communitybot.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "moderation_flags")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ModerationFlag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    /** Short reason label from the LLM classifier (e.g. "TOXIC", "HATE_SPEECH"). */
    @Column(name = "llm_reason")
    private String llmReason;

    @Column(name = "llm_explanation", columnDefinition = "TEXT")
    private String llmExplanation;

    @Column(name = "llm_confidence")
    private Double llmConfidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FlagStatus status = FlagStatus.PENDING;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    // ── Domain actions ────────────────────────────────────────────────────────

    public void approve(UUID reviewerId) {
        this.status     = FlagStatus.APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
        this.message.restore();
    }

    public void remove(UUID reviewerId) {
        this.status     = FlagStatus.REMOVED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
        this.message.hide();
    }
}
