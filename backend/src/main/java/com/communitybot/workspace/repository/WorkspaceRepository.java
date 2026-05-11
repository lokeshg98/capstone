package com.communitybot.workspace.repository;

import com.communitybot.workspace.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    boolean existsByOrganizationIdAndSlug(UUID orgId, String slug);

    Optional<Workspace> findByOrganizationIdAndSlug(UUID orgId, String slug);

    List<Workspace> findAllByOrganizationIdOrderByNameAsc(UUID orgId);

    /** Returns all workspaces the given user is a member of. */
    @Query("""
            SELECT wm.workspace
            FROM WorkspaceMember wm
            WHERE wm.user.id = :userId
              AND wm.workspace.organization.id = :orgId
            ORDER BY wm.workspace.name
            """)
    List<Workspace> findAllByMemberUserIdAndOrgId(UUID userId, UUID orgId);
}
