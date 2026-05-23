package com.communitybot.moderation.service;

import com.communitybot.ai.service.OpenAiChatService;
import com.communitybot.ai.service.OpenAiChatService.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * LLM judge that checks messages against workspace-specific community guidelines.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GuidelinesClassifierService {

    private final OpenAiChatService chatService;
    private final ObjectMapper      objectMapper;

    private static final int    MAX_TOKENS   = 150;
    private static final double TEMPERATURE  = 0.0;

    public ContentClassifierService.ClassificationResult classify(
            String messageBody,
            String guidelines,
            UUID authorId
    ) {
        if (messageBody == null || messageBody.isBlank()) {
            return ContentClassifierService.ClassificationResult.ofSafe();
        }
        if (messageBody.trim().split("\\s+").length < 3) {
            return ContentClassifierService.ClassificationResult.ofSafe();
        }

        String systemPrompt = """
                You are a content moderation assistant for an online community platform.
                Analyze the user message against the COMMUNITY GUIDELINES below.
                Return ONLY a JSON object — no prose, no markdown.

                JSON format (required):
                {"safe":true,"flagReason":"SAFE","confidence":0.95,"explanation":"brief reason"}

                Rules:
                - safe: true if acceptable under the guidelines, false if it violates them
                - flagReason: one of SAFE, TOXIC, HATE_SPEECH, HARASSMENT, SPAM, THREAT, OFF_TOPIC, IMPERSONATION
                - confidence: float 0.0–1.0
                - explanation: 1-2 sentences citing which guideline was violated (if any)

                COMMUNITY GUIDELINES:
                """ + guidelines;

        try {
            String raw = chatService.complete(
                    List.of(ChatMessage.system(systemPrompt), ChatMessage.user(messageBody)),
                    MAX_TOKENS,
                    TEMPERATURE,
                    authorId
            );
            return parse(raw);
        } catch (Exception e) {
            log.warn("Guidelines classification error: {}", e.getMessage());
            return ContentClassifierService.ClassificationResult.ofSafe();
        }
    }

    private ContentClassifierService.ClassificationResult parse(String rawResponse) {
        String json = rawResponse.trim()
                .replaceAll("(?s)^```json\\s*", "")
                .replaceAll("(?s)^```\\s*", "")
                .replaceAll("(?s)\\s*```$", "")
                .trim();
        try {
            JsonNode node = objectMapper.readTree(json);
            return new ContentClassifierService.ClassificationResult(
                    node.path("safe").asBoolean(true),
                    node.path("flagReason").asText("SAFE"),
                    node.path("confidence").asDouble(0.5),
                    node.path("explanation").asText("")
            );
        } catch (Exception e) {
            log.warn("Failed to parse guidelines classifier JSON: {}", rawResponse, e);
            return ContentClassifierService.ClassificationResult.ofSafe();
        }
    }
}
