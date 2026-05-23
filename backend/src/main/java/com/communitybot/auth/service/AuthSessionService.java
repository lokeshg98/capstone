package com.communitybot.auth.service;

import com.communitybot.auth.domain.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final JwtService jwtService;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    public AuthTokens issue(User user) {
        return new AuthTokens(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user)
        );
    }

    public void addRefreshCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refresh_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge((int) (refreshTokenExpiryMs / 1000));
        response.addCookie(cookie);
    }

    public void clearRefreshCookie(HttpServletResponse response) {
        Cookie clear = new Cookie("refresh_token", "");
        clear.setHttpOnly(true);
        clear.setSecure(true);
        clear.setPath("/api/auth/refresh");
        clear.setMaxAge(0);
        response.addCookie(clear);
    }

    public record AuthTokens(String accessToken, String refreshToken) {}
}
