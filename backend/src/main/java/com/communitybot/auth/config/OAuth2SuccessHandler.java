package com.communitybot.auth.config;

import com.communitybot.auth.domain.User;
import com.communitybot.auth.service.AuthenticatedPrincipal;
import com.communitybot.auth.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Called by Spring Security after a successful OAuth2 / OIDC login.
 * Mints our own JWTs, sets the refresh token in an HttpOnly cookie,
 * and redirects the browser to the frontend callback URL with the access token.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest  request,
            HttpServletResponse response,
            Authentication      authentication
    ) throws IOException {

        User user = ((AuthenticatedPrincipal) authentication.getPrincipal()).getUser();

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        addRefreshCookie(response, refreshToken);

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/auth/callback")
                .queryParam("token", accessToken)
                .build()
                .toUriString();

        log.debug("OAuth2 success — redirecting userId={} to frontend", user.getId());
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private void addRefreshCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refresh_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);               // sent only over HTTPS
        cookie.setPath("/api/auth/refresh");  // scoped so it isn't sent on every request
        cookie.setMaxAge((int) (refreshTokenExpiryMs / 1000));
        response.addCookie(cookie);
    }
}
