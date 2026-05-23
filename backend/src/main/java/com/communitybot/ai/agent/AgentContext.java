package com.communitybot.ai.agent;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Per-request agent context on the worker thread (Ask HTTP, SSE, or async @bot).
 */
public record AgentContext(
        UUID   workspaceId,
        UUID   userId,
        UUID   conversationId,
        boolean constrainedChannelMode,
        Consumer<AgentStreamEvent> streamSink,
        SseEmitter sseEmitter
) {
    public static AgentContext sync(UUID workspaceId, UUID userId, UUID conversationId) {
        return new AgentContext(workspaceId, userId, conversationId, false, null, null);
    }

    public static AgentContext channelBot(UUID workspaceId, UUID userId) {
        return new AgentContext(workspaceId, userId, null, true, null, null);
    }

    public static AgentContext publicAsk() {
        return new AgentContext(null, null, null, false, null, null);
    }

    public boolean isPublicMode() {
        return workspaceId == null;
    }

    public static AgentContext streaming(UUID workspaceId, UUID userId, UUID conversationId,
                                         Consumer<AgentStreamEvent> sink) {
        return new AgentContext(workspaceId, userId, conversationId, false, sink, null);
    }

    public void emit(AgentStreamEvent event) {
        if (streamSink != null) {
            streamSink.accept(event);
        }
    }
}
