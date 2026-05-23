package com.communitybot.auth.domain;

import com.communitybot.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_users_provider_subject",
        columnNames = {"oauth_provider", "oauth_subject"}
    )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Verified email from the OAuth provider. */
    @Column(nullable = false)
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", nullable = false)
    private OauthProvider oauthProvider;

    /** Stable unique ID from the identity provider (Google sub, GitHub id). */
    @Column(name = "oauth_subject", nullable = false)
    private String oauthSubject;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "status_message")
    private String statusMessage;

    @Column(name = "about_me", columnDefinition = "TEXT")
    private String aboutMe;

    @Column(columnDefinition = "TEXT")
    private String interests;

    @Column(name = "contact_info", columnDefinition = "TEXT")
    private String contactInfo;

    /** Called on each OAuth login to keep display info fresh. */
    public void updateProfile(String displayName, String avatarUrl) {
        this.displayName = displayName;
        this.avatarUrl   = avatarUrl;
    }

    public void updateUserProfile(String statusMessage, String aboutMe,
                                   String interests, String contactInfo) {
        this.statusMessage = statusMessage;
        this.aboutMe       = aboutMe;
        this.interests     = interests;
        this.contactInfo   = contactInfo;
    }
}
