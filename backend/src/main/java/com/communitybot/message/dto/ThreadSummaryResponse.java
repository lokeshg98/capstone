package com.communitybot.message.dto;

import java.time.Instant;
import java.util.UUID;

public record ThreadSummaryResponse(
        String  summaryBody,
        int     messageCount,
        Instant createdAt,
        UUID    botMessageId
) {}
