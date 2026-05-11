package com.communitybot.message.domain;

public enum MessageStatus {
    ACTIVE,   // visible to all channel members
    EDITED,   // body was changed after initial send
    FLAGGED,  // raised by the moderation worker; visible to moderators only
    HIDDEN,   // hidden from regular members; moderators can still see it
    DELETED   // soft-deleted by the author or a moderator
}
