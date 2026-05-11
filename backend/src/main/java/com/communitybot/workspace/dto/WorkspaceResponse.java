package com.communitybot.workspace.dto;

import com.communitybot.workspace.domain.Workspace;
import com.communitybot.workspace.domain.WorkspaceRole;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID          id,
        UUID          orgId,
        String        name,
        String        slug,
        String        description,
        WorkspaceRole myRole,
        Instant       createdAt
) {
    public static WorkspaceResponse from(Workspace ws, WorkspaceRole myRole) {
        return new WorkspaceResponse(
                ws.getId(),
                ws.getOrganization().getId(),
                ws.getName(),
                ws.getSlug(),
                ws.getDescription(),
                myRole,
                ws.getCreatedAt()
        );
    }
}
