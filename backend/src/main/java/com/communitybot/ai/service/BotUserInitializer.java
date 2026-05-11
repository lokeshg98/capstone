package com.communitybot.ai.service;

import com.communitybot.auth.domain.OauthProvider;
import com.communitybot.auth.domain.User;
import com.communitybot.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures a dedicated Bot user exists in the database.
 * This user is used as the author of AI-generated reply messages.
 * Runs once at application startup.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BotUserInitializer implements ApplicationRunner {

    public static final String BOT_EMAIL         = "bot@community-bot.internal";
    public static final String BOT_DISPLAY_NAME  = "Community Bot 🤖";
    public static final String BOT_OAUTH_SUBJECT = "community-bot-system-user";

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail(BOT_EMAIL).isEmpty()) {
            userRepository.save(
                    User.builder()
                            .email(BOT_EMAIL)
                            .displayName(BOT_DISPLAY_NAME)
                            .oauthProvider(OauthProvider.GOOGLE)   // placeholder provider
                            .oauthSubject(BOT_OAUTH_SUBJECT)
                            .active(true)
                            .build()
            );
            log.info("Created Community Bot system user");
        }
    }
}
