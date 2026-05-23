package com.communitybot.ai.agent;

/** Collects telemetry for the current agent invocation (citations, steps, proposals). */
public final class AgentRunStateHolder {

    private static final ThreadLocal<AgentRunState> TL = new ThreadLocal<>();

    private AgentRunStateHolder() {
    }

    public static void init() {
        TL.set(new AgentRunState());
    }

    public static AgentRunState get() {
        return TL.get();
    }

    public static void clear() {
        TL.remove();
    }
}
