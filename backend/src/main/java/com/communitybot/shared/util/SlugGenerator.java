package com.communitybot.shared.util;

import java.util.regex.Pattern;

/**
 * Converts arbitrary names into URL-safe slugs.
 *
 * "My Org!"  →  "my-org"
 * "  hello   world  "  →  "hello-world"
 */
public final class SlugGenerator {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private SlugGenerator() {}

    public static String from(String name) {
        String normalised = name.trim().toLowerCase();
        String slug = NON_ALPHANUMERIC.matcher(normalised).replaceAll("-");
        // trim leading/trailing dashes that may appear from punctuation at boundaries
        return slug.replaceAll("^-+|-+$", "");
    }
}
