package com.communitybot.moderation.domain;

public enum FlagStatus {
    PENDING,   // awaiting moderator review
    APPROVED,  // reviewed and restored (message set back to ACTIVE)
    REMOVED    // reviewed and removed (message set to HIDDEN)
}
