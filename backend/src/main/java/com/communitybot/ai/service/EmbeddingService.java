package com.communitybot.ai.service;

import com.communitybot.ai.config.OpenAiProperties;
import com.communitybot.ai.agent.AgentContextHolder;
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
 * Wraps the OpenAI Embeddings API ({@code POST /v1/embeddings}).
 * Returns a {@code float[1536]} for {@code text-embedding-3-small}.
 */
@Service
@Slf4j
public class EmbeddingService {

    private final RestClient        restClient;
    private final OpenAiProperties  props;
    private final LlmUsageService   llmUsageService;

    public EmbeddingService(
            @Qualifier("openAiRestClient") RestClient restClient,
            OpenAiProperties props,
            LlmUsageService llmUsageService
    ) {
        this.restClient       = restClient;
        this.props            = props;
        this.llmUsageService  = llmUsageService;
    }

    /**
     * Embed attributing usage to {@link AgentContextHolder} user when present, otherwise unrecorded.
     */
    public float[] embed(String text) {
        UUID ctxUser = AgentContextHolder.get() != null ? AgentContextHolder.get().userId() : null;
        return embed(text, ctxUser);
    }

    /** @param attributingUserId user to bill tokens against, or null to skip usage logging */
    public float[] embed(String text, UUID attributingUserId) {
        var request  = new EmbedRequest(props.getEmbeddingModel(), List.of(text));
        var response = restClient.post()
                .uri("/embeddings")
                .body(request)
                .retrieve()
                .body(EmbedResponse.class);

        if (response == null || response.data().isEmpty()) {
            throw new RuntimeException("Empty embedding response from OpenAI");
        }

        if (attributingUserId != null && response.usage() != null && response.usage().promptTokens() != null) {
            llmUsageService.record(
                    attributingUserId,
                    LlmUsageCategory.EMBEDDING,
                    props.getEmbeddingModel(),
                    response.usage().promptTokens(),
                    0);
        }

        List<Double> raw = response.data().get(0).embedding();
        float[] result = new float[raw.size()];
        for (int i = 0; i < raw.size(); i++) result[i] = raw.get(i).floatValue();
        log.debug("Embedded text ({} chars) → {} dimensions", text.length(), result.length);
        return result;
    }

    // ── OpenAI API shapes ─────────────────────────────────────────────────────

    private record EmbedRequest(String model, List<String> input) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EmbedResponse(List<EmbedData> data, Usage usage) {}

    private record EmbedData(List<Double> embedding) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("total_tokens") Integer totalTokens
    ) {}
}
