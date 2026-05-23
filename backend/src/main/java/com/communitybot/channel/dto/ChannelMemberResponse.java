package com.communitybot.channel.dto;

import com.communitybot.channel.domain.ChannelMember;
import com.communitybot.workspace.domain.WorkspaceMember;

import java.util.List;
import java.util.UUID;

public record ChannelMemberResponse(
        UUID        userId,
        UUID        memberId,
        String      displayName,
        String      avatarUrl,
        boolean     online,
        List<String> roles
) {
    public static ChannelMemberResponse from(ChannelMember cm, boolean online, List<String> roles) {
        return new ChannelMemberResponse(
                cm.getUser().getId(),
                cm.getId(),
                cm.getUser().getDisplayName(),
                cm.getUser().getAvatarUrl(),
                online,
                roles
        );
    }

    public static ChannelMemberResponse fromWorkspaceMember(
            WorkspaceMember wm, boolean online, List<String> roles
    ) {
        return new ChannelMemberResponse(
                wm.getUser().getId(),
                wm.getId(),
                wm.getUser().getDisplayName(),
                wm.getUser().getAvatarUrl(),
                online,
                roles
        );
    }
}
