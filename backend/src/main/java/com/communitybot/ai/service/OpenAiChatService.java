package com.communitybot.ai.service;

import com.communitybot.ai.config.OpenAiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Wraps the OpenAI Chat Completions API ({@code POST /v1/chat/completions}).
 */
@Service
@Slf4j
public class OpenAiChatService {

    private final RestClient       restClient;
    private final OpenAiProperties props;

    public OpenAiChatService(
            @Qualifier("openAiRestClient") RestClient restClient,
            OpenAiProperties props
    ) {
        this.restClient = restClient;
        this.props      = props;
    }

    public String complete(List<ChatMessage> messages) {
        return complete(messages, props.getMaxTokens(), props.getTemperature());
    }

    /** Overload with explicit token limit and temperature — used by the content classifier. */
    public String complete(List<ChatMessage> messages, int maxTokens, double temperature) {
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
    private record ChatResponse(List<ChatChoice> choices) {}
    private record ChatChoice(ChatMessage message) {}
}
