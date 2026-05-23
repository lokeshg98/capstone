package com.communitybot.workspace.dto;

import com.communitybot.workspace.domain.Workspace;
import com.communitybot.workspace.domain.WorkspaceMember;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkspaceResponse(
        UUID          id,
        UUID          orgId,
        String        name,
        String        slug,
        String        description,
        List<String>  myRoles,
        Instant       createdAt
) {
    public static WorkspaceResponse from(Workspace ws, List<String> myRoles) {
        return new WorkspaceResponse(
                ws.getId(),
                ws.getOrganization().getId(),
                ws.getName(),
                ws.getSlug(),
                ws.getDescription(),
                myRoles,
                ws.getCreatedAt()
        );
    }

    public static WorkspaceResponse from(Workspace ws, WorkspaceMember member) {
        List<String> roles = member.getRoles().stream()
                .map(r -> r.getName())
                .sorted()
                .toList();
        return from(ws, roles);
    }
}
