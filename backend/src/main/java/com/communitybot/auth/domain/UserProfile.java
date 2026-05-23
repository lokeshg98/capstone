package com.communitybot.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "user_profiles")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "status_message")
    private String statusMessage;

    @Column(name = "about_me", columnDefinition = "TEXT")
    private String aboutMe;

    @Column(length = 32)
    private String phone;

    @Column(name = "show_email", nullable = false)
    private boolean showEmail;

    @Column(name = "show_phone", nullable = false)
    private boolean showPhone;

    /** Comma-separated interest tags, e.g. "Java,Spring,DevOps". */
    @Column(columnDefinition = "TEXT")
    private String interests;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_mode", nullable = false, length = 32)
    private NotificationMode notificationMode;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public List<String> interestList() {
        if (interests == null || interests.isBlank()) {
            return List.of();
        }
        return Arrays.stream(interests.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public void update(
            String statusMessage,
            String aboutMe,
            String phone,
            boolean showEmail,
            boolean showPhone,
            String interests,
            NotificationMode notificationMode
    ) {
        this.statusMessage     = statusMessage != null ? statusMessage : this.statusMessage;
        this.aboutMe           = aboutMe;
        this.phone             = phone;
        this.showEmail         = showEmail;
        this.showPhone         = showPhone;
        this.interests         = interests;
        this.notificationMode  = notificationMode != null ? notificationMode : NotificationMode.ALL;
        this.updatedAt         = Instant.now();
    }
}
