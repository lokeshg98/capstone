package com.communitybot.auth.dto;

import com.communitybot.auth.domain.NotificationMode;
import com.communitybot.auth.domain.User;
import com.communitybot.auth.domain.UserProfile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserProfileDetailResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        String statusMessage,
        String aboutMe,
        String phone,
        boolean showEmail,
        boolean showPhone,
        List<String> interests,
        NotificationMode notificationMode,
        Instant profileUpdatedAt
) {
    public static UserProfileDetailResponse from(User user, UserProfile profile) {
        return new UserProfileDetailResponse(
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
                profile.getNotificationMode(),
                profile.getUpdatedAt()
        );
    }
}
