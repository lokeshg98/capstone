package com.communitybot.document.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits extracted document text into overlapping character windows.
 *
 * <ul>
 *   <li>Window size: 1 800 chars (~450 tokens) — safely under the 8 192-token limit of
 *       {@code text-embedding-3-small}.</li>
 *   <li>Overlap: 200 chars — preserves sentence context across boundaries.</li>
 *   <li>Split point: last sentence-ending punctuation within the window, so chunks
 *       don't cut mid-sentence when possible.</li>
 * </ul>
 */
@Service
public class ChunkingService {

    private static final int WINDOW  = 1800;
    private static final int OVERLAP = 200;

    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + WINDOW, text.length());

            // Try to cut at a sentence boundary so we don't split mid-sentence
            if (end < text.length()) {
                end = lastSentenceEnd(text, start, end);
            }

            String chunk = text.substring(start, end).strip();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            // Move forward, stepping back by the overlap amount
            start = end - OVERLAP;
            if (start < 0 || start >= text.length()) break;
        }

        return chunks;
    }

    /** Scans backwards from {@code end} to find the last '.', '!', or '?' in the window. */
    private int lastSentenceEnd(String text, int start, int end) {
        for (int i = end - 1; i > start + (WINDOW / 2); i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?') return i + 1;
        }
        return end;
    }
}
