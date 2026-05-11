package com.communitybot.scheduling.dto;

import com.communitybot.scheduling.domain.ScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateScheduledPostRequest(
        @NotNull UUID         channelId,
        @NotBlank @Size(max = 4000) String body,
        @NotNull ScheduleType scheduleType,
        Instant               fireAt,          // required for ONE_SHOT
        String                cronExpression   // required for CRON (Spring 6-field format)
) {}
