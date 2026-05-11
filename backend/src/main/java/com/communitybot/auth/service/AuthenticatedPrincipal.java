package com.communitybot.auth.service;

import com.communitybot.auth.domain.User;

/**
 * Marker interface implemented by both OAuth2 and OIDC user wrappers.
 * Allows {@link com.communitybot.auth.config.OAuth2SuccessHandler} to extract
 * the application {@link User} without knowing which provider was used.
 */
public interface AuthenticatedPrincipal {
    User getUser();
}
