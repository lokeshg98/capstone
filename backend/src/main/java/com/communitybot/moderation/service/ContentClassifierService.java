package com.communitybot.moderation.service;

import com.communitybot.ai.service.OpenAiChatService;
import com.communitybot.ai.service.OpenAiChatService.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Uses {@code gpt-4o-mini} as an LLM judge to classify whether a message
 * violates community guidelines.
 *
 * <p>The LLM is instructed to respond with a strict JSON object only.
 * Temperature is set to 0 for deterministic, cost-efficient output.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContentClassifierService {

    private final OpenAiChatService chatService;
    private final ObjectMapper      objectMapper;

    private static final int    CLASSIFIER_MAX_TOKENS = 150;
    private static final double CLASSIFIER_TEMPERATURE = 0.0;

    private static final String SYSTEM_PROMPT = """
            You are a content moderation assistant for an online community platform.
            Analyze the user message and return ONLY a JSON object — no prose, no markdown.

            JSON format (required):
            {"safe":true,"flagReason":"SAFE","confidence":0.95,"explanation":"brief reason"}

            Rules:
            - safe: true if the message is acceptable, false if it violates guidelines
            - flagReason: one of SAFE, TOXIC, HATE_SPEECH, HARASSMENT, SPAM, THREAT
            - confidence: float 0.0–1.0 (how confident you are in this classification)
            - explanation: 1-2 sentences explaining your decision

            Flag as unsafe (safe=false) for: explicit threats, targeted harassment, slurs,
            severe profanity directed at a person, or phishing/spam links.
            Be lenient with mild frustration, disagreements, and off-topic content.
            """;

    public ClassificationResult classify(String messageBody) {
        // Skip very short messages — not enough signal
        if (messageBody.trim().split("\\s+").length < 4) {
            return ClassificationResult.ofSafe();
        }

        List<ChatMessage> messages = List.of(
                ChatMessage.system(SYSTEM_PROMPT),
                ChatMessage.user(messageBody)
        );

        try {
            String raw = chatService.complete(messages, CLASSIFIER_MAX_TOKENS, CLASSIFIER_TEMPERATURE);
            return parse(raw);
        } catch (Exception e) {
            log.warn("Content classification error: {}", e.getMessage());
            return ClassificationResult.ofSafe();  // fail-open: don't block messages on classifier errors
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ClassificationResult parse(String rawResponse) {
        // Strip potential markdown fences
        String json = rawResponse.trim()
                .replaceAll("(?s)^```json\\s*", "")
                .replaceAll("(?s)^```\\s*",     "")
                .replaceAll("(?s)\\s*```$",     "")
                .trim();
        try {
            JsonNode node = objectMapper.readTree(json);
            return new ClassificationResult(
                    node.path("safe").asBoolean(true),
                    node.path("flagReason").asText("SAFE"),
                    node.path("confidence").asDouble(0.5),
                    node.path("explanation").asText("")
            );
        } catch (Exception e) {
            log.warn("Failed to parse classifier JSON: {}", rawResponse, e);
            return ClassificationResult.ofSafe();
        }
    }

    // ── Result type ───────────────────────────────────────────────────────────

    public record ClassificationResult(boolean safe, String flagReason, double confidence, String explanation) {
        public static ClassificationResult ofSafe() {
            return new ClassificationResult(true, "SAFE", 1.0, "");
        }
    }
}
