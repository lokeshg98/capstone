package com.communitybot.scheduling.dto;

import java.util.List;

public record WeeklyDigestRunResponse(
        int usersProcessed,
        int digestsSent,
        int skipped,
        List<String> errors
) {}
