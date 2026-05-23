package com.communitybot.ai.config;

import com.communitybot.ai.agent.AgentContextHolder;
import com.communitybot.ai.agent.LongTermMemoryService;
import com.communitybot.ai.service.EmbeddingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class AgentEmbeddingConfig {

    @Bean
    LongTermMemoryService.EmbeddingClient longTermEmbeddingClient(EmbeddingService embeddingService) {
        return text -> {
            var ctx = AgentContextHolder.get();
            UUID uid = ctx != null ? ctx.userId() : null;
            return embeddingService.embed(text, uid);
        };
    }
}
