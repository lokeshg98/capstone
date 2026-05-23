package com.communitybot.auth.dto;

import com.communitybot.auth.domain.User;

import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID   id,
        String email,
        String displayName,
        String avatarUrl,
        String statusMessage,
        String aboutMe,
        String interests,
        String contactInfo,
        List<OrgMembership> organizations,
        List<WorkspaceMembership> workspaces
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getStatusMessage(),
                user.getAboutMe(),
                user.getInterests(),
                user.getContactInfo(),
                List.of(),
                List.of()
        );
    }

    public static UserProfileResponse full(User user,
                                            List<OrgMembership> orgs,
                                            List<WorkspaceMembership> workspaces) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getStatusMessage(),
                user.getAboutMe(),
                user.getInterests(),
                user.getContactInfo(),
                orgs,
                workspaces
        );
    }

    public record OrgMembership(UUID orgId, String orgName, String role) {}
    public record WorkspaceMembership(UUID workspaceId, String workspaceName, UUID orgId, List<String> roles) {}
}
