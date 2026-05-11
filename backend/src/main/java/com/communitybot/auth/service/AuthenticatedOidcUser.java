package com.communitybot.auth.service;

import com.communitybot.auth.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Map;

/**
 * Wraps the provider's {@link OidcUser} (used for Google) and bundles the
 * persisted application {@link User} so the success handler can mint a JWT.
 */
@Getter
public class AuthenticatedOidcUser implements OidcUser, AuthenticatedPrincipal {

    private final OidcUser delegate;
    private final User     user;

    public AuthenticatedOidcUser(OidcUser delegate, User user) {
        this.delegate = delegate;
        this.user     = user;
    }

    @Override public Map<String, Object>                     getClaims()      { return delegate.getClaims(); }
    @Override public OidcUserInfo                            getUserInfo()    { return delegate.getUserInfo(); }
    @Override public OidcIdToken                             getIdToken()     { return delegate.getIdToken(); }
    @Override public Map<String, Object>                     getAttributes()  { return delegate.getAttributes(); }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return delegate.getAuthorities(); }
    @Override public String                                  getName()        { return user.getId().toString(); }
}
