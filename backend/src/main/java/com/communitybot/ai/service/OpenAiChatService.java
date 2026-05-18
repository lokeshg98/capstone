package com.communitybot.ai.service;

import com.communitybot.ai.config.OpenAiProperties;
import com.communitybot.ai.usage.LlmUsageCategory;
import com.communitybot.ai.usage.LlmUsageService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

/**
 * Wraps the OpenAI Chat Completions API ({@code POST /v1/chat/completions}).
 */
@Service
@Slf4j
public class OpenAiChatService {

    private final RestClient       restClient;
    private final OpenAiProperties props;
    private final LlmUsageService  llmUsageService;

    public OpenAiChatService(
            @Qualifier("openAiRestClient") RestClient restClient,
            OpenAiProperties props,
            LlmUsageService llmUsageService
    ) {
        this.restClient      = restClient;
        this.props           = props;
        this.llmUsageService = llmUsageService;
    }

    public String complete(List<ChatMessage> messages) {
        return complete(messages, props.getMaxTokens(), props.getTemperature(), null);
    }

    /** Overload with explicit token limit and temperature — used by the content classifier. */
    public String complete(List<ChatMessage> messages, int maxTokens, double temperature) {
        return complete(messages, maxTokens, temperature, null);
    }

    public String complete(List<ChatMessage> messages, int maxTokens, double temperature, UUID attributingUserId) {
        var request  = new ChatRequest(
                props.getChatModel(),
                messages,
                maxTokens,
                temperature
        );
        var response = restClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .body(ChatResponse.class);

        if (response == null || response.choices().isEmpty()) {
            throw new RuntimeException("Empty chat completion response from OpenAI");
        }

        if (attributingUserId != null && response.usage() != null) {
            Usage u = response.usage();
            int pt = u.promptTokens() == null ? 0 : u.promptTokens();
            int ct = u.completionTokens() == null ? 0 : u.completionTokens();
            llmUsageService.record(attributingUserId, LlmUsageCategory.CLASSIFIER, props.getChatModel(), pt, ct);
        }

        String content = response.choices().get(0).message().content();
        log.debug("Chat completion: {} chars", content.length());
        return content;
    }

    // ── Message factory helpers ───────────────────────────────────────────────

    public record ChatMessage(String role, String content) {
        public static ChatMessage system(String content) { return new ChatMessage("system", content); }
        public static ChatMessage user(String content)   { return new ChatMessage("user",   content); }
    }

    // ── OpenAI API shapes ─────────────────────────────────────────────────────

    private record ChatRequest(String model, List<ChatMessage> messages, int max_tokens, double temperature) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatResponse(List<ChatChoice> choices, Usage usage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens
    ) {}

    private record ChatChoice(ChatMessage message) {}
}
