package com.communitybot.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from {@code app.agent.*} in application.yml.
 */
@Data
@ConfigurationProperties(prefix = "app.agent")
public class AgentProperties {

    private int  maxSequentialTools            = 8;
    private int  maxSequentialToolsConstrained = 4;
    private int  shortTermMemoryMaxMessages     = 24;
    private int  redisMemoryTtlHours           = 168;
    private int  longTermMemoryTopK            = 3;
    /** Max agent invocations (Ask + channel @bot) per workspace per day. */
    private int  dailyWorkspaceLimit         = 500;
    private String tavilyApiKey              = "";
    /** When true, only workspace ADMINS may call web search. Moderators excluded. */
    private boolean webSearchAdminOnly       = true;
}
