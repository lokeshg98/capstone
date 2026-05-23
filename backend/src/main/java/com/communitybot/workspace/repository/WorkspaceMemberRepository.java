package com.communitybot.workspace.repository;

import com.communitybot.workspace.domain.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    List<WorkspaceMember> findByUserId(UUID userId);

    @Query("""
            SELECT wm FROM WorkspaceMember wm
            JOIN FETCH wm.user
            WHERE wm.workspace.id = :workspaceId
            """)
    List<WorkspaceMember> findAllByWorkspaceId(UUID workspaceId);
}
