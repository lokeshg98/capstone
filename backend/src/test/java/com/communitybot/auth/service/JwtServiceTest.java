package com.communitybot.auth.service;

import com.communitybot.auth.domain.OauthProvider;
import com.communitybot.auth.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    // 64-byte Base64 secret for tests
    private static final String TEST_SECRET =
            "dGVzdFNlY3JldEtleUZvckp3dFNlcnZpY2VUZXN0c09ubHlEb05vdFVzZUluUHJvZA==";

    private JwtService jwtService;
    private User       testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret",            TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiryMs",  900_000L);   // 15 min
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiryMs", 604_800_000L); // 7 days

        testUser = User.builder()
                .oauthProvider(OauthProvider.GOOGLE)
                .oauthSubject("12345")
                .email("alice@example.com")
                .displayName("Alice")
                .build();
        // Force a known ID via reflection (builder won't set it — the DB normally does)
        ReflectionTestUtils.setField(testUser, "id", UUID.randomUUID());
    }

    @Test
    void accessToken_isValid_andCarriesCorrectUserId() {
        String token = jwtService.generateAccessToken(testUser);

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.isAccessToken(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(testUser.getId());
    }

    @Test
    void refreshToken_isValid_butNotAnAccessToken() {
        String token = jwtService.generateRefreshToken(testUser);

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.isAccessToken(token)).isFalse();
    }

    @Test
    void tamperedToken_isInvalid() {
        String token   = jwtService.generateAccessToken(testUser);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThat(jwtService.isValid(tampered)).isFalse();
    }
}
