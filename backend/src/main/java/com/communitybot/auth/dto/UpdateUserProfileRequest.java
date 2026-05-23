package com.communitybot.auth.dto;

import com.communitybot.auth.domain.NotificationMode;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateUserProfileRequest(
        @Size(max = 200) String statusMessage,
        @Size(max = 2000) String aboutMe,
        @Size(max = 32) String phone,
        Boolean showEmail,
        Boolean showPhone,
        List<@Size(max = 50) String> interests,
        NotificationMode notificationMode
) {}
