package com.communitybot.channel.domain;

public enum ChannelType {
    PUBLIC,     // visible and joinable by all workspace members
    PRIVATE,    // invite-only
    DM,         // direct message between exactly 2 users
    GROUP_DM    // direct message among 3+ users
}
