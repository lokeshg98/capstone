package com.communitybot.channel.dto;

import java.util.Set;

public record UpdateChannelRestrictionsRequest(
        boolean roleRestricted,
        Set<String> accessibleRoles
) {}
