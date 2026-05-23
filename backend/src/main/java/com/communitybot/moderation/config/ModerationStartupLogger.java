package com.communitybot.moderation.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ModerationStartupLogger {

    private final ModerationProperties moderationProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void logModerationState() {
        if (moderationProperties.isClassificationEnabled()) {
            log.info(
                    "Content moderation ENABLED (OpenAI={}, guidelines={}, hideOnFlag={})",
                    moderationProperties.isOpenaiModerationEnabled(),
                    moderationProperties.isGuidelinesCheckEnabled(),
                    moderationProperties.isHideOnFlag()
            );
        } else {
            log.warn(
                    "Content moderation DISABLED — harmful messages will NOT be flagged. "
                            + "Set MODERATION_ENABLED=true in infra/.env and restart."
            );
        }
    }
}
