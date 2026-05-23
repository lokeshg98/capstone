package com.communitybot.scheduling.dto;

import java.util.UUID;

public record N8nWeeklyDigestRequest(
        UUID workspaceId,
        Integer periodDays,
        Boolean dryRun
) {}
