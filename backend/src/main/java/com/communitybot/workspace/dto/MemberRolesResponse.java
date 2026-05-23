package com.communitybot.workspace.dto;

import com.communitybot.workspace.domain.WorkspaceMember;

import java.util.List;
import java.util.UUID;

public record MemberRolesResponse(
        UUID        memberId,
        UUID        userId,
        String      userDisplayName,
        String      userEmail,
        List<String> roles
) {
    public static MemberRolesResponse from(WorkspaceMember member) {
        return new MemberRolesResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getDisplayName(),
                member.getUser().getEmail(),
                member.getRoles().stream().map(r -> r.getName()).sorted().toList()
        );
    }
}
