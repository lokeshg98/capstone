package com.communitybot.ai.agent;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class AgentMetrics {

    private final AtomicLong              completions = new AtomicLong();
    private final Map<String, AtomicLong> toolCalls   = new ConcurrentHashMap<>();

    public void recordCompletion() {
        completions.incrementAndGet();
    }

    public void recordTool(String toolName) {
        toolCalls.computeIfAbsent(toolName, k -> new AtomicLong()).incrementAndGet();
    }

    public Map<String, Object> snapshot() {
        Map<String, Long> tools = toolCalls.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
        return Map.of(
                "completions", completions.get(),
                "tools", tools
        );
    }
}
