package com.communitybot.auth.dto;

import com.communitybot.auth.domain.User;

import java.util.UUID;

/**
 * Public user profile sent to the frontend.
 */
public record UserProfileResponse(
        UUID   id,
        String email,
        String displayName,
        String avatarUrl
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl()
        );
    }
}
