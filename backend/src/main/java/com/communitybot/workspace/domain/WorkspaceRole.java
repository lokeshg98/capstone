package com.communitybot.workspace.domain;

public enum WorkspaceRole {
    ADMIN,      // manage channels, members, bot config
    MODERATOR,  // review flagged content, manage members
    MEMBER      // post and read messages
}
