package com.communitybot.emoji.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.joypixels")
@Getter
@Setter
public class JoyPixelsProperties {

    private boolean enabled = true;

    /** JoyPixels emoji-toolkit JSON index (open-source, MIT). */
    private String emojiJsonUrl =
            "https://cdn.jsdelivr.net/npm/emoji-toolkit@9.0.0/emoji.json";

    private String cdnBase = "https://cdn.jsdelivr.net/npm/emoji-toolkit@9.0.0";

    /** PNG size segment in CDN path: 32, 64, or 128. */
    private int pngSize = 64;

    /** Use LLM to interpret natural-language emoji queries (e.g. "celebration"). */
    private boolean aiSearchEnabled = true;

    private int defaultLimit = 24;

    /** Reserved for JoyPixels enterprise API when available. */
    private String apiKey = "";
}
