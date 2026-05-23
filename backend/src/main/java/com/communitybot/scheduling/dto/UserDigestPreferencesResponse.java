package com.communitybot.scheduling.dto;

import java.time.Instant;
import java.util.UUID;

public record UserDigestPreferencesResponse(
        UUID userId,
        boolean weeklyDigestEnabled,
        Instant updatedAt
) {}
