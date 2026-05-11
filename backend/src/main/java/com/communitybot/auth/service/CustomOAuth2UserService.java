package com.communitybot.auth.service;

import com.communitybot.auth.domain.OauthProvider;
import com.communitybot.auth.domain.User;
import com.communitybot.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles non-OIDC OAuth2 providers (currently GitHub).
 * Upserts the application user on every login so display info stays fresh.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(request);

        String registrationId = request.getClientRegistration().getRegistrationId();
        OauthProvider provider = OauthProvider.valueOf(registrationId.toUpperCase());
        UserAttributes attrs   = UserAttributes.of(provider, oauth2User.getAttributes());

        User user = upsert(provider, attrs);
        log.debug("OAuth2 login: provider={} userId={}", provider, user.getId());

        return new AuthenticatedOAuth2User(oauth2User, user);
    }

    private User upsert(OauthProvider provider, UserAttributes attrs) {
        return userRepository
                .findByOauthProviderAndOauthSubject(provider, attrs.subject())
                .map(existing -> {
                    existing.updateProfile(attrs.name(), attrs.avatarUrl());
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(attrs.email())
                                .displayName(attrs.name())
                                .avatarUrl(attrs.avatarUrl())
                                .oauthProvider(provider)
                                .oauthSubject(attrs.subject())
                                .build()
                ));
    }
}
