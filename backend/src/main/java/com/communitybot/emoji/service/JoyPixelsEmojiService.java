package com.communitybot.emoji.service;

import com.communitybot.ai.service.OpenAiChatService;
import com.communitybot.ai.service.OpenAiChatService.ChatMessage;
import com.communitybot.emoji.config.JoyPixelsProperties;
import com.communitybot.emoji.dto.EmojiSearchResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JoyPixels emoji search backed by the official open {@code emoji-toolkit} index,
 * with optional LLM semantic matching for natural-language queries.
 *
 * <p>JoyPixels does not publish a standalone public REST search API; this service
 * mirrors EmojiCopy-style search using their MIT-licensed metadata + CDN PNGs.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JoyPixelsEmojiService {

    private record EmojiEntry(
            String codePointKey,
            String name,
            String shortname,
            List<String> keywords,
            String category,
            String fullyQualified
    ) {}

    private final JoyPixelsProperties properties;
    private final ObjectMapper        objectMapper;
    private final OpenAiChatService   openAiChatService;

    private List<EmojiEntry> catalog = List.of();
    private Map<String, EmojiEntry> byShortname = Map.of();

    @PostConstruct
    void loadCatalog() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            RestClient client = RestClient.create();
            String json = client.get().uri(properties.getEmojiJsonUrl()).retrieve().body(String.class);
            if (json == null || json.isBlank()) {
                log.warn("JoyPixels emoji index empty at {}", properties.getEmojiJsonUrl());
                return;
            }
            JsonNode root = objectMapper.readTree(json);
            List<EmojiEntry> entries = new ArrayList<>();
            Map<String, EmojiEntry> shortMap = new HashMap<>();

            root.fields().forEachRemaining(field -> {
                JsonNode node = field.getValue();
                if (node.path("display").asInt(1) != 1) {
                    return;
                }
                String fq = node.path("code_points").path("fully_qualified").asText(field.getKey());
                String shortname = node.path("shortname").asText("");
                List<String> keywords = new ArrayList<>();
                node.path("keywords").forEach(k -> keywords.add(k.asText()));
                EmojiEntry entry = new EmojiEntry(
                        field.getKey(),
                        node.path("name").asText(""),
                        shortname,
                        keywords,
                        node.path("category").asText(""),
                        fq
                );
                entries.add(entry);
                if (!shortname.isBlank()) {
                    shortMap.put(shortname.toLowerCase(Locale.ROOT), entry);
                }
            });

            catalog = List.copyOf(entries);
            byShortname = Map.copyOf(shortMap);
            log.info("Loaded {} JoyPixels emoji entries", catalog.size());
        } catch (Exception e) {
            log.error("Failed to load JoyPixels emoji index: {}", e.getMessage());
        }
    }

    public List<EmojiSearchResult> search(String query, int limit, UUID userId) {
        if (!properties.isEnabled() || catalog.isEmpty()) {
            return List.of();
        }
        int cap = limit > 0 ? Math.min(limit, 50) : properties.getDefaultLimit();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        List<EmojiSearchResult> results;
        if (q.isEmpty()) {
            results = defaultQuickPicks();
        } else if (q.startsWith(":") && q.endsWith(":")) {
            results = lookupShortname(q, cap);
        } else {
            results = keywordSearch(q, cap);
            if (results.size() < 5 && properties.isAiSearchEnabled() && q.length() >= 2) {
                results = merge(results, aiSearch(q, cap, userId), cap);
            }
        }
        return results.stream().limit(cap).toList();
    }

    private List<EmojiSearchResult> keywordSearch(String q, int cap) {
        return catalog.stream()
                .map(e -> new Scored(toResult(e), score(e, q)))
                .filter(s -> s.score > 0)
                .sorted(Comparator.comparingInt(Scored::score).reversed())
                .limit(cap)
                .map(s -> s.result)
                .toList();
    }

    private static int score(EmojiEntry e, String q) {
        int s = 0;
        if (e.name().contains(q)) s += 10;
        if (e.shortname().toLowerCase(Locale.ROOT).contains(q)) s += 8;
        for (String kw : e.keywords()) {
            if (kw.contains(q)) s += 5;
        }
        if (e.name().startsWith(q)) s += 4;
        return s;
    }

    private List<EmojiSearchResult> lookupShortname(String shortname, int cap) {
        EmojiEntry exact = byShortname.get(shortname.toLowerCase(Locale.ROOT));
        if (exact != null) {
            return List.of(toResult(exact));
        }
        return keywordSearch(shortname.replace(":", ""), cap);
    }

    private List<EmojiSearchResult> aiSearch(String query, int cap, UUID userId) {
        try {
            String prompt = """
                    The user is searching for emoji matching: "%s"
                    Return ONLY a JSON array of JoyPixels shortnames (e.g. [":tada:",":heart:"]).
                    Pick up to %d diverse matches. No markdown.
                    """.formatted(query, cap);
            String raw = openAiChatService.complete(
                    List.of(ChatMessage.system("You map natural language to emoji shortnames."),
                            ChatMessage.user(prompt)),
                    120,
                    0.0,
                    userId
            );
            String json = raw.trim()
                    .replaceAll("(?s)^```json\\s*", "")
                    .replaceAll("(?s)^```\\s*", "")
                    .replaceAll("(?s)\\s*```$", "")
                    .trim();
            List<String> shortnames = objectMapper.readValue(json, new TypeReference<>() {});
            return shortnames.stream()
                    .map(s -> byShortname.get(s.toLowerCase(Locale.ROOT)))
                    .filter(Objects::nonNull)
                    .map(this::toResult)
                    .distinct()
                    .limit(cap)
                    .toList();
        } catch (Exception e) {
            log.debug("JoyPixels AI search fallback for '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    private List<EmojiSearchResult> defaultQuickPicks() {
        List<String> picks = List.of(
                ":thumbsup:", ":heart:", ":fire:", ":joy:", ":thinking:",
                ":tada:", ":100:", ":eyes:", ":pray:", ":rocket:"
        );
        return picks.stream()
                .map(s -> byShortname.get(s))
                .filter(Objects::nonNull)
                .map(this::toResult)
                .toList();
    }

    private EmojiSearchResult toResult(EmojiEntry e) {
        return new EmojiSearchResult(
                codePointsToUnicode(e.fullyQualified()),
                e.name(),
                e.shortname(),
                e.category(),
                pngUrl(e.fullyQualified())
        );
    }

    String pngUrl(String fullyQualified) {
        return properties.getCdnBase() + "/png/" + properties.getPngSize() + "/" + fullyQualified + ".png";
    }

    static String codePointsToUnicode(String fullyQualified) {
        StringBuilder sb = new StringBuilder();
        for (String part : fullyQualified.split("-")) {
            sb.appendCodePoint(Integer.parseInt(part, 16));
        }
        return sb.toString();
    }

    private static List<EmojiSearchResult> merge(
            List<EmojiSearchResult> a,
            List<EmojiSearchResult> b,
            int cap
    ) {
        LinkedHashMap<String, EmojiSearchResult> map = new LinkedHashMap<>();
        a.forEach(r -> map.put(r.unicode(), r));
        b.forEach(r -> map.putIfAbsent(r.unicode(), r));
        return map.values().stream().limit(cap).collect(Collectors.toList());
    }

    private record Scored(EmojiSearchResult result, int score) {}
}
