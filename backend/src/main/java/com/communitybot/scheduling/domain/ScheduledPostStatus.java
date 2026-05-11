package com.communitybot.scheduling.domain;

public enum ScheduledPostStatus {
    PENDING,    // waiting for delivery
    SENT,       // delivered (one-shot) or no more occurrences (cron)
    CANCELLED,  // manually cancelled before delivery
    ERROR       // delivery attempted but failed
}
