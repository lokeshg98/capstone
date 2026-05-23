package com.communitybot.scheduling.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.n8n")
@Getter
@Setter
public class N8nProperties {

    /** Shared secret for n8n webhook calls (header {@code X-N8n-Api-Key}). */
    private String apiKey = "";

    private boolean enabled = true;

    /** Default look-back window when n8n omits {@code periodDays}. */
    private int defaultPeriodDays = 7;

    /** Max messages pulled per channel when building a digest transcript. */
    private int maxMessagesPerChannel = 40;
}
