package com.communitybot.auth.service;

import com.communitybot.auth.domain.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Generates and validates HS256 JWTs.
 *
 * Token types:
 *   ACCESS  — short-lived (15 min), carried in the Authorization header.
 *   REFRESH — long-lived (7 days), stored in an HttpOnly cookie.
 */
@Service
@Slf4j
public class JwtService {

    private static final String CLAIM_TYPE  = "type";
    private static final String CLAIM_EMAIL = "email";
    private static final String TYPE_ACCESS  = "ACCESS";
    private static final String TYPE_REFRESH = "REFRESH";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    public String generateAccessToken(User user) {
        return build(user, accessTokenExpiryMs, TYPE_ACCESS);
    }

    public String generateRefreshToken(User user) {
        return build(user, refreshTokenExpiryMs, TYPE_REFRESH);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(claims(token).getSubject());
    }

    public boolean isValid(String token) {
        try {
            claims(token);
            return true;
        } catch (JwtException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(claims(token).get(CLAIM_TYPE, String.class));
    }

    // -------------------------------------------------------------------------

    private String build(User user, long expiryMs, String type) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claims(Map.of(CLAIM_EMAIL, user.getEmail(), CLAIM_TYPE, type))
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiryMs))
                .signWith(signingKey())
                .compact();
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}
