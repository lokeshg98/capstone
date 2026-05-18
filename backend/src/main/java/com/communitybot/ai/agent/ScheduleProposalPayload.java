package com.communitybot.ai.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScheduleProposalPayload(
        UUID   channelId,
        String body,
        String scheduleType,
        String fireAtIso,
        String cronExpression
) {}
