package com.communitybot.ai.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * Streaming AiService facade (Ask Bot panel SSE).
 */
public interface StreamingCommunityAssistant {
    TokenStream chat(@MemoryId String memoryId, @UserMessage String userMessage);
}
