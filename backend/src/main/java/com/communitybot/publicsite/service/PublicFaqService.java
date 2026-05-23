package com.communitybot.publicsite.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PublicFaqService {

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
                entries.add(new FaqEntry(q, a));
            }
        }
        return entries;
    }

    public String searchForAgent(String query, int maxChars) {
        String q = query == null ? "" : query.toLowerCase();
        StringBuilder sb = new StringBuilder();
        for (FaqEntry e : loadEntries(50)) {
            if (q.isBlank()
                    || e.question().toLowerCase().contains(q)
                    || e.answer().toLowerCase().contains(q)) {
                sb.append("Q: ").append(e.question()).append("\nA: ").append(e.answer()).append("\n\n");
            }
            if (sb.length() >= maxChars) break;
        }
        if (sb.isEmpty()) {
            return loadEntries(5).stream()
                    .map(e -> "Q: " + e.question() + "\nA: " + e.answer())
                    .reduce((a, b) -> a + "\n\n" + b)
                    .orElse("No FAQ content available.");
        }
        return sb.length() <= maxChars ? sb.toString() : sb.substring(0, maxChars) + "…";
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
