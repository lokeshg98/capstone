package com.communitybot.publicsite.dto;

import java.util.List;

public record PlatformInfoResponse(
        String title,
        String tagline,
        String summary,
        List<FeatureCard> features
) {
    public record FeatureCard(String title, String description, String icon) {}
}
