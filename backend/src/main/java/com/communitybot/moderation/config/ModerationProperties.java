package com.communitybot.moderation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.moderation")
@Getter
@Setter
public class ModerationProperties {

    private boolean classificationEnabled = true;

    /** Minimum confidence (0–1) for custom guidelines LLM judge to flag a message. */
    private double confidenceThreshold = 0.7;

    /** Use OpenAI Moderation API ({@code POST /v1/moderations}). */
    private boolean openaiModerationEnabled = true;

    /** Second-pass LLM check against {@link com.communitybot.moderation.service.CommunityGuidelinesService}. */
    private boolean guidelinesCheckEnabled = true;

    /** Immediately hide flagged messages from all channel members. */
    private boolean hideOnFlag = true;

    /** OpenAI moderation model (e.g. omni-moderation-latest). */
    private String openaiModerationModel = "omni-moderation-latest";
}
