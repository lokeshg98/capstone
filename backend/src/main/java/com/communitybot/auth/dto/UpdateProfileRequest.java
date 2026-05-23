package com.communitybot.auth.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 200) String statusMessage,
        @Size(max = 500) String aboutMe,
        @Size(max = 500) String interests,
        @Size(max = 500) String contactInfo
) {}
