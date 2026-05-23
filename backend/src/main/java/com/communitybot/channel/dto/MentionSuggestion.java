package com.communitybot.channel.dto;

import java.util.UUID;

public record MentionSuggestion(
        UUID   userId,
        String displayName,
        String avatarUrl,
        boolean isBot
) {}
