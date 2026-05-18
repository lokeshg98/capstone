package com.communitybot.ai.agent;

/** Thread-local holder for {@link AgentContext} during agent / tool execution. */
public final class AgentContextHolder {

    private static final ThreadLocal<AgentContext> CTX = new ThreadLocal<>();

    private AgentContextHolder() {
    }

    public static void set(AgentContext ctx) {
        CTX.set(ctx);
    }

    public static AgentContext get() {
        return CTX.get();
    }

    public static void clear() {
        CTX.remove();
    }
}
