package com.communitybot.auth.service;

import com.communitybot.auth.domain.OauthProvider;

import java.util.Map;

/**
 * Normalises provider-specific attribute maps into a single, type-safe structure.
 * Adding a new OAuth provider means adding one more case in {@link #of}.
 */
record UserAttributes(String subject, String email, String name, String avatarUrl) {

    static UserAttributes of(OauthProvider provider, Map<String, Object> attrs) {
        return switch (provider) {
            case GOOGLE -> fromGoogle(attrs);
            case GITHUB -> fromGithub(attrs);
        };
    }

    private static UserAttributes fromGoogle(Map<String, Object> attrs) {
        return new UserAttributes(
                (String) attrs.get("sub"),
                (String) attrs.get("email"),
                (String) attrs.get("name"),
                (String) attrs.get("picture")
        );
    }

    private static UserAttributes fromGithub(Map<String, Object> attrs) {
        // GitHub's integer id is the stable subject; login is mutable
        String subject = String.valueOf(attrs.get("id"));
        // GitHub may return null email if the user's email is private
        String email = (String) attrs.getOrDefault("email", subject + "@github.noreply");
        return new UserAttributes(
                subject,
                email,
                (String) attrs.get("name"),
                (String) attrs.get("avatar_url")
        );
    }
}
