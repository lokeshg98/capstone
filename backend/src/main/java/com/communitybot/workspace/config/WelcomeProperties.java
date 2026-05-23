package com.communitybot.workspace.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.welcome")
@Getter
@Setter
public class WelcomeProperties {

    private boolean enabled = true;

    /** Use LLM to personalize welcome text from profile + interests. */
    private boolean personalizationEnabled = true;

    private String defaultTemplate =
            "👋 Welcome to the community, {name}! We're glad you're here. "
                    + "Introduce yourself in #general and say hi to the team.";
}
