package com.communitybot.auth.controller;

import com.communitybot.auth.domain.User;
import com.communitybot.auth.dto.AuthTokenResponse;
import com.communitybot.auth.dto.UserProfileResponse;
import com.communitybot.auth.service.JwtService;
import com.communitybot.auth.service.AuthSessionService;
import com.communitybot.auth.service.LocalAuthService;
import com.communitybot.auth.service.UserService;
import com.communitybot.shared.dto.ApiResponse;
import com.communitybot.shared.security.CurrentUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService         jwtService;
    private final AuthSessionService  sessionService;
    private final LocalAuthService    localAuthService;
    private final UserService         userService;

    /** Returns the profile of the currently authenticated user. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMe(@CurrentUser UUID userId) {
        User user = userService.getOrThrow(userId);
        return ResponseEntity.ok(ApiResponse.ok(UserProfileResponse.from(user)));
    }

    /**
     * Rotates an expired access token using the refresh-token cookie.
     * The cookie is sent automatically by the browser because its path is /api/auth/refresh.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(HttpServletRequest request) {
        String refreshToken = cookieValue(request, "refresh_token");

        if (refreshToken == null || !jwtService.isValid(refreshToken)) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Invalid or missing refresh token"));
        }

        UUID userId  = jwtService.extractUserId(refreshToken);
        User user    = userService.getOrThrow(userId);
        String token = jwtService.generateAccessToken(user);

        return ResponseEntity.ok(ApiResponse.ok(new AuthTokenResponse(token)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> localLogin(
            @Valid @RequestBody LocalLoginRequest req,
            HttpServletResponse response
    ) {
        User user = localAuthService.login(req.email(), req.password());
        AuthSessionService.AuthTokens tokens = sessionService.issue(user);
        sessionService.addRefreshCookie(response, tokens.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(new AuthTokenResponse(tokens.accessToken())));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> register(
            @Valid @RequestBody LocalRegisterRequest req,
            HttpServletResponse response
    ) {
        User user = localAuthService.register(req.email(), req.password(), req.displayName());
        AuthSessionService.AuthTokens tokens = sessionService.issue(user);
        sessionService.addRefreshCookie(response, tokens.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(new AuthTokenResponse(tokens.accessToken())));
    }

    /** Clears the refresh-token cookie (browser-side logout). */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        sessionService.clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    private String cookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    public record LocalLoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record LocalRegisterRequest(
            @Email @NotBlank String email,
            @NotBlank String password,
            String displayName
    ) {}
}
