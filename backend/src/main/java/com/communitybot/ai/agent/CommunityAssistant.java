package com.communitybot.ai.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * Non-streaming AiService facade for the multi-agent orchestrator.
 */
public interface CommunityAssistant {
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);
}
