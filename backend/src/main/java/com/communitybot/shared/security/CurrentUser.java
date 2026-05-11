package com.communitybot.shared.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * Injects the authenticated user's {@link java.util.UUID} into a controller method parameter.
 *
 * <pre>
 * {@code @GetMapping("/me")}
 * public ResponseEntity<?> getMe(@CurrentUser UUID userId) { ... }
 * </pre>
 *
 * The expression reads {@code principal.username} (the UUID string stored in
 * the {@code UserDetails} username field) and converts it.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "T(java.util.UUID).fromString(#this.username)")
public @interface CurrentUser {
}
