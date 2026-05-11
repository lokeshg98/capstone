package com.communitybot.auth.service;

import com.communitybot.auth.domain.OauthProvider;
import com.communitybot.auth.domain.User;
import com.communitybot.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles OIDC providers (currently Google).
 * Mirrors the upsert logic of {@link CustomOAuth2UserService} but works with
 * {@link OidcUser}, which carries the OIDC ID token in addition to user-info claims.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(request);

        UserAttributes attrs = UserAttributes.of(OauthProvider.GOOGLE, oidcUser.getAttributes());
        User user = upsert(attrs);
        log.debug("OIDC login: provider=GOOGLE userId={}", user.getId());

        return new AuthenticatedOidcUser(oidcUser, user);
    }

    private User upsert(UserAttributes attrs) {
        return userRepository
                .findByOauthProviderAndOauthSubject(OauthProvider.GOOGLE, attrs.subject())
                .map(existing -> {
                    existing.updateProfile(attrs.name(), attrs.avatarUrl());
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(attrs.email())
                                .displayName(attrs.name())
                                .avatarUrl(attrs.avatarUrl())
                                .oauthProvider(OauthProvider.GOOGLE)
                                .oauthSubject(attrs.subject())
                                .build()
                ));
    }
}
