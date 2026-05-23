package com.communitybot.scheduling.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_digest_preferences")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserDigestPreferences {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "weekly_digest_enabled", nullable = false)
    private boolean weeklyDigestEnabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void setWeeklyDigestEnabled(boolean enabled) {
        this.weeklyDigestEnabled = enabled;
        this.updatedAt = Instant.now();
    }
}
