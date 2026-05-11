package com.communitybot.ai.service;

import com.communitybot.ai.config.OpenAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Wraps the OpenAI Embeddings API ({@code POST /v1/embeddings}).
 * Returns a {@code float[1536]} for {@code text-embedding-3-small}.
 */
@Service
@Slf4j
public class EmbeddingService {

    private final RestClient        restClient;
    private final OpenAiProperties  props;

    public EmbeddingService(
            @Qualifier("openAiRestClient") RestClient restClient,
            OpenAiProperties props
    ) {
        this.restClient = restClient;
        this.props      = props;
    }

    public float[] embed(String text) {
        var request  = new EmbedRequest(props.getEmbeddingModel(), List.of(text));
        var response = restClient.post()
                .uri("/embeddings")
                .body(request)
                .retrieve()
                .body(EmbedResponse.class);

        if (response == null || response.data().isEmpty()) {
            throw new RuntimeException("Empty embedding response from OpenAI");
        }

        List<Double> raw = response.data().get(0).embedding();
        float[] result = new float[raw.size()];
        for (int i = 0; i < raw.size(); i++) result[i] = raw.get(i).floatValue();
        log.debug("Embedded text ({} chars) → {} dimensions", text.length(), result.length);
        return result;
    }

    // ── OpenAI API shapes ─────────────────────────────────────────────────────

    private record EmbedRequest(String model, List<String> input) {}
    private record EmbedResponse(List<EmbedData> data) {}
    private record EmbedData(List<Double> embedding) {}
}
