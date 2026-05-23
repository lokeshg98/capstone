package com.communitybot.scheduling.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Validates {@code X-N8n-Api-Key} for {@code /api/internal/n8n/**} webhook routes.
 */
@Component
@RequiredArgsConstructor
public class N8nApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-N8n-Api-Key";

    private final N8nProperties n8nProperties;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/internal/n8n/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {

        if (!n8nProperties.isEnabled()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "n8n integration disabled");
            return;
        }

        String configured = n8nProperties.getApiKey();
        if (!StringUtils.hasText(configured)) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "n8n API key not configured");
            return;
        }

        String provided = request.getHeader(API_KEY_HEADER);
        if (!configured.equals(provided)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid n8n API key");
            return;
        }

        var auth = new UsernamePasswordAuthenticationToken(
                "n8n",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_N8N"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }
}
