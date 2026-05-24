package com.communitybot.publicsite.service;

import com.communitybot.ai.agent.UserFacingResponseSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PublicFaqService {

    private final UserFacingResponseSanitizer responseSanitizer;

    private static final Set<String> STOP_WORDS = Set.of(
            "does", "the", "and", "for", "with", "this", "that", "application", "support",
            "your", "our", "app", "are", "can", "how", "what", "when", "where", "who", "why",
            "have", "has", "was", "were", "will", "would", "could", "should", "from", "into",
            "about", "any", "all", "not", "you", "use", "using", "get", "got"
    );

    private static final long MIN_MATCH_SCORE = 3L;

    private static final Pattern OFF_TOPIC = Pattern.compile(
            "\\b(weather|forecast|temperature|humidity|raining|rain|snow|sunny|climate|"
                    + "stock\\s+price|bitcoin|crypto|election|who\\s+won|sports\\s+score|"
                    + "recipe|cook|restaurant|near\\s+me)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final String NO_FAQ_MATCH = """
            I couldn't find that in the Community Bot FAQ. \
            Try asking about sign-in, channels, Ask Bot, moderation, file uploads, or roles. \
            If you need live web information (e.g. weather), a workspace admin can enable web search in Ask Bot.""";

    private static final String OFF_TOPIC_REPLY = """
            Community Bot doesn't provide live weather, news, or general web lookups in FAQ mode. \
            Ask me about the platform — for example: "How does Ask Bot work?" or "How does moderation work?" \
            Workspace admins can enable web search in Ask Bot for broader questions.""";

    private static final Pattern QA = Pattern.compile(
            "\\*\\*Q:\\s*(.+?)\\*\\*\\s*\\R(.+?)(?=\\R\\R|\\*\\*Q:|\\z)",
            Pattern.DOTALL
    );

    public record FaqEntry(String question, String answer) {}

    public List<FaqEntry> loadEntries(int limit) {
        String text = readFaqMarkdown();
        List<FaqEntry> entries = new ArrayList<>();
        Matcher m = QA.matcher(text);
        while (m.find() && entries.size() < limit) {
            String q = m.group(1).trim();
            String a = m.group(2).trim().replaceAll("\\s+", " ");
            if (!q.isBlank() && !a.isBlank()) {
                entries.add(new FaqEntry(q, responseSanitizer.sanitize(a)));
            }
        }
        return entries;
    }

    public String searchForAgent(String query, int maxChars) {
        List<FaqEntry> ranked = rankEntries(query, MIN_MATCH_SCORE);
        if (ranked.isEmpty()) {
            return "No FAQ entries matched this query. "
                    + "Try keywords about Community Bot features (sign-in, channels, Ask Bot, moderation).";
        }
        StringBuilder sb = new StringBuilder();
        for (FaqEntry e : ranked) {
            sb.append("Q: ").append(e.question()).append("\nA: ").append(e.answer()).append("\n\n");
            if (sb.length() >= maxChars) break;
        }
        if (sb.isEmpty()) {
            return "No FAQ content available.";
        }
        return sb.length() <= maxChars ? sb.toString().trim() : sb.substring(0, maxChars).trim() + "…";
    }

    /** Single user-facing answer when the LLM agent is unavailable or no good FAQ match exists. */
    public String answerForUser(String query) {
        if (isOffTopicQuestion(query)) {
            return OFF_TOPIC_REPLY;
        }
        FaqEntry best = findBestMatch(query);
        return best != null ? best.answer() : NO_FAQ_MATCH;
    }

    /** Best single FAQ entry for public demo fallback when the LLM agent is unavailable. */
    public FaqEntry findBestMatch(String query) {
        List<FaqEntry> ranked = rankEntries(query, MIN_MATCH_SCORE);
        return ranked.isEmpty() ? null : ranked.getFirst();
    }

    private boolean isOffTopicQuestion(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        return OFF_TOPIC.matcher(query).find();
    }

    private List<FaqEntry> rankEntries(String query) {
        return rankEntries(query, 1L);
    }

    private List<FaqEntry> rankEntries(String query, long minScore) {
        String q = query == null ? "" : query.toLowerCase();
        List<String> tokens = Pattern.compile("\\w+")
                .matcher(q)
                .results()
                .map(m -> m.group().toLowerCase())
                .filter(t -> t.length() > 2 && !STOP_WORDS.contains(t))
                .distinct()
                .toList();

        List<FaqEntry> entries = loadEntries(50);
        if (tokens.isEmpty()) {
            return List.of();
        }

        boolean wantsGiphy = q.contains("giphy") || q.contains("gif");
        boolean wantsEmoji = q.contains("emoji") || q.contains("emojis") || q.contains("sticker");
        boolean wantsVacation = q.contains("vacation") || q.contains("away")
                || q.contains("out of office") || q.contains("ooo");

        record Scored(FaqEntry entry, long score) {}

        return entries.stream()
                .map(e -> {
                    String question = e.question().toLowerCase();
                    String answer = e.answer().toLowerCase();
                    String hay = question + " " + answer;
                    long score = 0;
                    for (String token : tokens) {
                        if (question.contains(token)) {
                            score += 3;
                        } else if (answer.contains(token)) {
                            score += 1;
                        }
                    }
                    if (q.length() > 3 && hay.contains(q)) {
                        score += 10;
                    }
                    if (wantsGiphy && (hay.contains("giphy") || hay.contains("gif"))) {
                        score += 20;
                    }
                    if (wantsEmoji && (hay.contains("emoji") || hay.contains("reaction") || hay.contains("react"))) {
                        score += 15;
                    }
                    if (wantsVacation && (hay.contains("vacation") || hay.contains("away") || hay.contains("office"))) {
                        score += 20;
                    }
                    return new Scored(e, score);
                })
                .filter(s -> s.score() >= minScore)
                .sorted(Comparator.comparingLong(Scored::score).reversed())
                .map(Scored::entry)
                .toList();
    }

    private String readFaqMarkdown() {
        Path repoRoot = Path.of("FAQ.md");
        if (Files.isRegularFile(repoRoot)) {
            try {
                return Files.readString(repoRoot, StandardCharsets.UTF_8);
            } catch (IOException ignored) {
                // fall through
            }
        }
        try {
            return new ClassPathResource("FAQ.md").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            return """
                    **Q: What is Community Bot?**
                    An AI-powered Slack-style community platform with moderation, RAG FAQ answers, and real-time chat.

                    **Q: How do I sign in?**
                    Use Google or GitHub OAuth from the home page.
                    """;
        }
    }
}
