package com.communitybot.channel.dto;

import com.communitybot.channel.domain.Channel;
import com.communitybot.channel.domain.ChannelType;

import java.time.Instant;
import java.util.UUID;

public record ChannelResponse(
        UUID        id,
        UUID        workspaceId,
        String      name,
        String      slug,
        ChannelType type,
        String      description,
        boolean     isMember,
        Instant     createdAt
) {
    public static ChannelResponse from(Channel channel, boolean isMember) {
        return new ChannelResponse(
                channel.getId(),
                channel.getWorkspace().getId(),
                channel.getName(),
                channel.getSlug(),
                channel.getType(),
                channel.getDescription(),
                isMember,
                channel.getCreatedAt()
        );
    }
}
