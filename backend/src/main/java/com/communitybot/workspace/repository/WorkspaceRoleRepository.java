package com.communitybot.workspace.repository;

import com.communitybot.workspace.domain.WorkspaceRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRoleRepository extends JpaRepository<WorkspaceRoleEntity, UUID> {

    List<WorkspaceRoleEntity> findAllByWorkspaceIdOrderByNameAsc(UUID workspaceId);

    Optional<WorkspaceRoleEntity> findByWorkspaceIdAndName(UUID workspaceId, String name);

    boolean existsByWorkspaceIdAndName(UUID workspaceId, String name);
}
