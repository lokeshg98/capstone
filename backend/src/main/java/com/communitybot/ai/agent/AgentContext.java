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
        boolean constrainedChannelMode,
        Consumer<AgentStreamEvent> streamSink,
        SseEmitter sseEmitter
) {
    public static AgentContext sync(UUID workspaceId, UUID userId) {
        return new AgentContext(workspaceId, userId, false, null, null);
    }

    public static AgentContext channelBot(UUID workspaceId, UUID userId) {
        return new AgentContext(workspaceId, userId, true, null, null);
    }

    public static AgentContext streaming(UUID workspaceId, UUID userId, Consumer<AgentStreamEvent> sink) {
        return new AgentContext(workspaceId, userId, false, sink, null);
    }

    public void emit(AgentStreamEvent event) {
        if (streamSink != null) {
            streamSink.accept(event);
        }
    }
}
