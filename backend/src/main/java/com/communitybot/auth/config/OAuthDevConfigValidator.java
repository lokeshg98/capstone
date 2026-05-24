package com.communitybot.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Warns in dev when OAuth client IDs are still placeholders (Google login will fail with invalid_client).
 */
@Component
@Profile("dev")
@Slf4j
public class OAuthDevConfigValidator implements ApplicationRunner {

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Override
    public void run(ApplicationArguments args) {
        if (googleClientId == null || googleClientId.isBlank()
                || googleClientId.contains("placeholder")
                || googleClientId.startsWith("dev-")) {
            log.warn("""
                    Google OAuth is not configured — Sign in with Google will fail (invalid_client).
                    Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET in infra/.env, then restart the backend.
                    For demos without OAuth setup, use local accounts: user@communitybot.local / User123!
                    See RUNBOOK.md Step 1b for Google Cloud Console setup.""");
        }
    }
}
