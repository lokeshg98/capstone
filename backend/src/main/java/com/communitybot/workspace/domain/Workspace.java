package com.communitybot.workspace.domain;

import com.communitybot.organization.domain.Organization;
import com.communitybot.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "workspaces",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_workspaces_org_slug",
        columnNames = {"org_id", "slug"}
    )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Workspace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String name;

    /** Unique within the parent organisation (not globally). */
    @Column(nullable = false)
    private String slug;

    private String description;

    @Column(name = "welcome_message_template", columnDefinition = "TEXT")
    private String welcomeMessageTemplate;

    public void update(String name, String slug, String description) {
        this.name        = name;
        this.slug        = slug;
        this.description = description;
    }

    @OneToMany(mappedBy = "workspace", fetch = FetchType.LAZY)
    @Builder.Default
    private List<WorkspaceMember> members = new ArrayList<>();

    public void setWelcomeMessageTemplate(String template) {
        this.welcomeMessageTemplate = template;
    }
}
