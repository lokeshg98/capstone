package com.communitybot.scheduling.domain;

public enum ScheduleType {
    ONE_SHOT,   // fires once at a specific instant
    CRON        // fires repeatedly according to a Spring cron expression
}
