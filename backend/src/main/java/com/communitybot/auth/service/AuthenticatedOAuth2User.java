package com.communitybot.auth.service;

import com.communitybot.auth.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * Wraps the provider's {@link OAuth2User} (used for GitHub) and bundles the
 * persisted application {@link User} so the success handler can mint a JWT.
 */
@Getter
public class AuthenticatedOAuth2User implements OAuth2User, AuthenticatedPrincipal {

    private final OAuth2User  delegate;
    private final User        user;

    public AuthenticatedOAuth2User(OAuth2User delegate, User user) {
        this.delegate = delegate;
        this.user     = user;
    }

    @Override public Map<String, Object>                      getAttributes()  { return delegate.getAttributes(); }
    @Override public Collection<? extends GrantedAuthority>  getAuthorities() { return delegate.getAuthorities(); }
    @Override public String                                   getName()        { return user.getId().toString(); }
}
