package com.communitybot.actuator;

import com.communitybot.ai.agent.AgentMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Endpoint(id = "agents")
@RequiredArgsConstructor
public class AgentsEndpoint {

    private final AgentMetrics agentMetrics;

    @ReadOperation
    public Map<String, Object> agents() {
        return agentMetrics.snapshot();
    }
}
