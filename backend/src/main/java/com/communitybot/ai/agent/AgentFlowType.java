package com.communitybot.ai.agent;

import com.communitybot.workspace.domain.WorkspaceMember;
import com.communitybot.workspace.repository.WorkspaceMemberRepository;

import java.util.UUID;

/**
 * LangGraph4j routing targets for member vs moderator/admin vs channel vs public flows.
 */
public enum AgentFlowType {
    PUBLIC("public_agent"),
    CHANNEL("channel_agent"),
    MEMBER("member_agent"),
    MODERATOR("moderator_agent");

    private final String nodeName;

    AgentFlowType(String nodeName) {
        this.nodeName = nodeName;
    }

    public String nodeName() {
        return nodeName;
    }

    public static AgentFlowType fromContext(AgentContext ctx, WorkspaceMemberRepository members) {
        if (ctx == null || ctx.isPublicMode()) {
            return PUBLIC;
        }
        if (ctx.constrainedChannelMode()) {
            return CHANNEL;
        }
        UUID wsId = ctx.workspaceId();
        UUID userId = ctx.userId();
        if (wsId == null || userId == null) {
            return PUBLIC;
        }
        WorkspaceMember member = members.findByWorkspaceIdAndUserId(wsId, userId)
                .orElse(null);
        if (member != null && (member.hasRole("Admin") || member.hasRole("Moderator"))) {
            return MODERATOR;
        }
        return MEMBER;
    }
}
