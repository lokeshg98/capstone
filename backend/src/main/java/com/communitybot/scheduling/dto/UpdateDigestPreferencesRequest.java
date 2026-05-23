package com.communitybot.scheduling.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateDigestPreferencesRequest(
        @NotNull Boolean weeklyDigestEnabled
) {}
