package com.communitybot.workspace.dto;

import com.communitybot.workspace.domain.WorkspaceRoleEntity;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceRoleResponse(
        UUID   id,
        String name,
        boolean isSystem,
        Instant createdAt
) {
    public static WorkspaceRoleResponse from(WorkspaceRoleEntity entity) {
        return new WorkspaceRoleResponse(
                entity.getId(),
                entity.getName(),
                entity.isSystem(),
                entity.getCreatedAt()
        );
    }
}
