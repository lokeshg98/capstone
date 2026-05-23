package com.communitybot.ai.service;

import com.communitybot.ai.agent.MultiAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Backwards-compatible facade for callers that used the legacy single-shot RAG service.
 * All logic is delegated to {@link MultiAgentService}.
 */
@Service
@RequiredArgsConstructor
public class RagService {

    private final MultiAgentService multiAgentService;

    public MultiAgentService.AgentAnswer ask(UUID workspaceId, UUID userId, String question, String conversationId) {
        return multiAgentService.answerSync(workspaceId, userId, conversationId, question, false);
    }
}
