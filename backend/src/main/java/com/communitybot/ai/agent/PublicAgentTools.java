package com.communitybot.ai.agent;

import com.communitybot.publicsite.service.PublicFaqService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublicAgentTools {

    private final PublicFaqService publicFaqService;

    @Tool("Search public FAQ documentation about Community Bot (no login required).")
    public String searchPublicFaq(String query) {
        AgentRunState state = AgentRunStateHolder.get();
        if (state != null) {
            state.addStep("public_faq", query);
        }
        return publicFaqService.searchForAgent(query, 4000);
    }
}
