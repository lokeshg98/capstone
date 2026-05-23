package com.communitybot.auth.dto;

import com.communitybot.auth.domain.User;
import com.communitybot.auth.domain.UserProfile;

import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID   id,
        String email,
        String displayName,
        String avatarUrl,
        String statusMessage,
        String aboutMe,
        String phone,
        boolean showEmail,
        boolean showPhone,
        List<String> interests,
        List<OrgMembership> organizations,
        List<WorkspaceMembership> workspaces
) {
    public static UserProfileResponse from(User user, UserProfile profile) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                profile.getStatusMessage(),
                profile.getAboutMe(),
                profile.getPhone(),
                profile.isShowEmail(),
                profile.isShowPhone(),
                profile.interestList(),
                List.of(),
                List.of()
        );
    }

    public static UserProfileResponse full(User user, UserProfile profile,
                                            List<OrgMembership> orgs,
                                            List<WorkspaceMembership> workspaces) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                profile.getStatusMessage(),
                profile.getAboutMe(),
                profile.getPhone(),
                profile.isShowEmail(),
                profile.isShowPhone(),
                profile.interestList(),
                orgs,
                workspaces
        );
    }

    public record OrgMembership(UUID orgId, String orgName, String role) {}
    public record WorkspaceMembership(UUID workspaceId, String workspaceName, UUID orgId, List<String> roles) {}
}
