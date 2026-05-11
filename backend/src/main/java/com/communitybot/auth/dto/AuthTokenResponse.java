package com.communitybot.auth.dto;

/**
 * Returned by the /refresh endpoint so the client can store a new access token.
 */
public record AuthTokenResponse(String accessToken) {
}
