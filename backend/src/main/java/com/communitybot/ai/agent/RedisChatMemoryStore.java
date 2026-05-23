package com.communitybot.ai.agent;

import com.communitybot.ai.config.AgentProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper        objectMapper;
    private final AgentProperties     props;

    private static final String PREFIX = "agent:chatmem:";

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = PREFIX + memoryId;
        String json = redis.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, String>> raw = objectMapper.readValue(json, new TypeReference<>() {});
            List<ChatMessage> out = new ArrayList<>();
            for (Map<String, String> m : raw) {
                String role = m.get("role");
                String text = m.get("text");
                if (text == null) continue;
                if ("USER".equals(role)) {
                    out.add(UserMessage.from(text));
                } else if ("AI".equals(role)) {
                    out.add(new AiMessage(text));
                }
            }
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = PREFIX + memoryId;
        List<Map<String, String>> raw = new ArrayList<>();
        for (ChatMessage m : messages) {
            if (m instanceof UserMessage um) {
                Map<String, String> row = new LinkedHashMap<>();
                row.put("role", "USER");
                row.put("text", userText(um));
                raw.add(row);
            } else if (m instanceof AiMessage am) {
                if (am.hasToolExecutionRequests()) {
                    continue;
                }
                Map<String, String> row = new LinkedHashMap<>();
                row.put("role", "AI");
                row.put("text", am.text());
                raw.add(row);
            }
        }
        try {
            String json = objectMapper.writeValueAsString(raw);
            redis.opsForValue().set(key, json, Duration.ofHours(props.getRedisMemoryTtlHours()));
        } catch (Exception ignored) {
            // best-effort persistence
        }
    }

    private static String userText(UserMessage um) {
        if (um.hasSingleText()) {
            return um.singleText();
        }
        return um.contents().toString();
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redis.delete(PREFIX + memoryId);
    }
}
