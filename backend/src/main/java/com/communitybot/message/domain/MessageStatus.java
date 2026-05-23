package com.communitybot.message.domain;

public enum MessageStatus {
    ACTIVE,   // visible to all channel members
    EDITED,   // body was changed after initial send
    FLAGGED,  // legacy: auto-moderation now sets HIDDEN immediately
    HIDDEN,   // hidden from all channel members; visible in moderation queue
    DELETED   // soft-deleted by the author or a moderator
}
