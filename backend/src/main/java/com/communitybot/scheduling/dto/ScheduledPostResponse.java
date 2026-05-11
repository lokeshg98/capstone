package com.communitybot.scheduling.dto;

import com.communitybot.scheduling.domain.ScheduledPost;
import com.communitybot.scheduling.domain.ScheduledPostStatus;
import com.communitybot.scheduling.domain.ScheduleType;

import java.time.Instant;
import java.util.UUID;

public record ScheduledPostResponse(
        UUID                id,
        UUID                workspaceId,
        UUID                channelId,
        String              channelName,
        String              body,
        ScheduleType        scheduleType,
        String              cronExpression,
        Instant             nextFireAt,
        ScheduledPostStatus status,
        Instant             lastSentAt,
        Instant             createdAt
) {
    public static ScheduledPostResponse from(ScheduledPost p) {
        return new ScheduledPostResponse(
                p.getId(),
                p.getWorkspace().getId(),
                p.getChannel().getId(),
                p.getChannel().getName(),
                p.getBody(),
                p.getScheduleType(),
                p.getCronExpression(),
                p.getNextFireAt(),
                p.getStatus(),
                p.getLastSentAt(),
                p.getCreatedAt()
        );
    }
}
