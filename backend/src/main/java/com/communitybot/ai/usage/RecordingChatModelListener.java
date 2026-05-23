package com.communitybot.ai.usage;

import com.communitybot.ai.agent.AgentContextHolder;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Records per-request token usage from LangChain4j chat / streaming chat models.
 * Uses {@link AgentContextHolder} for the attributing user (Ask Bot / @bot flows).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecordingChatModelListener implements ChatModelListener {

    private static final String ATTR_USER_ID = "llmUsageUserId";

    private final LlmUsageService llmUsageService;

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        UUID userId = null;
        var ctx = AgentContextHolder.get();
        if (ctx != null) {
            userId = ctx.userId();
        }
        if (userId != null) {
            requestContext.attributes().put(ATTR_USER_ID, userId);
        }
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        Object raw = responseContext.attributes().get(ATTR_USER_ID);
        if (!(raw instanceof UUID userId)) {
            return;
        }
        TokenUsage tu = responseContext.chatResponse().metadata().tokenUsage();
        if (tu == null) {
            return;
        }
        int in  = tu.inputTokenCount() == null ? 0 : tu.inputTokenCount();
        int out = tu.outputTokenCount() == null ? 0 : tu.outputTokenCount();
        String model = responseContext.chatResponse().metadata().modelName();
        if (model == null || model.isBlank()) {
            model = "(unknown)";
        }
        llmUsageService.record(userId, LlmUsageCategory.CHAT, model, in, out);
    }
}
