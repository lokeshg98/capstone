package com.communitybot.ai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class OpenAiConfig {

    private final OpenAiProperties props;

    /**
     * A {@link RestClient} pre-configured with the OpenAI base URL and the
     * Bearer token from {@code app.openai.api-key}.
     * Injected by name ("openAiRestClient") to avoid conflicting with other RestClient beans.
     */
    @Bean("openAiRestClient")
    public RestClient openAiRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
