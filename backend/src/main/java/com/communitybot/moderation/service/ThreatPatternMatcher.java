package com.communitybot.moderation.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Fast local scan for obvious threats — runs before paid API calls and works
 * when OpenAI is unavailable or moderation API errors out.
 */
@Component
public class ThreatPatternMatcher {

    private static final Pattern[] THREAT_PATTERNS = {
            Pattern.compile("(?i)\\b(i\\s*'?ll|i\\s+will|im\\s+going\\s+to|gonna)\\s+(kill|murder|hurt|shoot|stab)\\s+(you|u|them|him|her)\\b"),
            Pattern.compile("(?i)\\b(kill|murder|hurt|shoot|stab)\\s+(you|u|yourself)\\b"),
            Pattern.compile("(?i)\\bdeath\\s+threat\\b"),
            Pattern.compile("(?i)\\bi\\s+hope\\s+you\\s+die\\b"),
    };

    public boolean matches(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.trim();
        for (Pattern pattern : THREAT_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                return true;
            }
        }
        return false;
    }
}
