package com.communitybot.scheduling.domain;

import com.communitybot.channel.domain.Channel;
import com.communitybot.shared.domain.BaseEntity;
import com.communitybot.workspace.domain.Workspace;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduled_posts")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScheduledPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 20)
    private ScheduleType scheduleType;

    /** Spring 6-field cron expression (second minute hour dom month dow). Null for ONE_SHOT. */
    @Column(name = "cron_expression")
    private String cronExpression;

    /** When the post should next fire. Updated after each CRON delivery. */
    @Column(name = "next_fire_at", nullable = false)
    private Instant nextFireAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ScheduledPostStatus status = ScheduledPostStatus.PENDING;

    @Column(name = "last_sent_at")
    private Instant lastSentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // ── Domain actions ────────────────────────────────────────────────────────

    public void markSent() {
        this.status     = ScheduledPostStatus.SENT;
        this.lastSentAt = Instant.now();
    }

    public void markError(String message) {
        this.status       = ScheduledPostStatus.ERROR;
        this.errorMessage = message;
    }

    public void cancel() {
        this.status = ScheduledPostStatus.CANCELLED;
    }

    /**
     * For CRON posts: record delivery and advance {@code nextFireAt} to the next occurrence.
     * If no further occurrences exist, marks the post as SENT.
     */
    public void rescheduleFromCron() {
        this.lastSentAt = Instant.now();
        CronExpression cron = CronExpression.parse(this.cronExpression);
        ZonedDateTime  next = cron.next(ZonedDateTime.now(ZoneOffset.UTC));
        if (next != null) {
            this.nextFireAt = next.toInstant();
        } else {
            this.status = ScheduledPostStatus.SENT;
        }
    }
}
