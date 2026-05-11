package com.communitybot.workspace.domain;

import com.communitybot.auth.domain.User;
import com.communitybot.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspace_bans",
       uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "user_id", "active"}))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkspaceBan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "banned_by", nullable = false)
    private UUID bannedBy;

    @Column(columnDefinition = "TEXT")
    private String reason;

    /** Null means the ban is permanent. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    public void lift() {
        this.active = false;
    }

    public boolean isEffectivelyActive() {
        return active && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }
}
