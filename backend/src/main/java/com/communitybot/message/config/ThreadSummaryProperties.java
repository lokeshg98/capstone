package com.communitybot.message.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.thread-summary")
@Getter
@Setter
public class ThreadSummaryProperties {

    /** Generate Otter-style digests when a thread reaches this many messages (root + replies). */
    private int minMessages = 10;

    private boolean enabled = true;
}
