package com.communitybot.channel.domain;

import com.communitybot.auth.domain.User;
import com.communitybot.shared.domain.BaseEntity;
import com.communitybot.workspace.domain.Workspace;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "channels",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_channels_ws_slug",
        columnNames = {"workspace_id", "slug"}
    )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Channel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false)
    private String name;

    /** URL-safe identifier — unique within the parent workspace. */
    @Column(nullable = false)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ChannelType type = ChannelType.PUBLIC;

    @Column(name = "role_restricted", nullable = false)
    @Builder.Default
    private boolean roleRestricted = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "channel_accessible_roles",
        joinColumns = @JoinColumn(name = "channel_id")
    )
    @Column(name = "role_name")
    @Builder.Default
    private Set<String> accessibleRoles = new HashSet<>();

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    public void update(String name, String description) {
        this.name        = name;
        this.description = description;
    }

    public void updateRestrictions(boolean restricted, Set<String> roles) {
        this.roleRestricted  = restricted;
        this.accessibleRoles = roles != null ? roles : Set.of();
    }
}
