package com.communitybot.workspace.event;

import java.util.UUID;

public record UserJoinedWorkspaceEvent(UUID workspaceId, UUID userId) {}
