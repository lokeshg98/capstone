package com.communitybot.auth.service;

import com.communitybot.auth.domain.LocalCredential;
import com.communitybot.auth.domain.OauthProvider;
import com.communitybot.auth.domain.User;
import com.communitybot.auth.repository.LocalCredentialRepository;
import com.communitybot.auth.repository.UserRepository;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalAuthService {

    private final UserRepository userRepository;
    private final LocalCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(String email, String password, String displayName) {
        String normalizedEmail = normalizeEmail(email);
        validatePassword(password);

        if (credentialRepository.existsByEmail(normalizedEmail) || userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = userRepository.save(
                User.builder()
                        .email(normalizedEmail)
                        .displayName(normalizeDisplayName(displayName, normalizedEmail))
                        .avatarUrl(null)
                        .oauthProvider(OauthProvider.LOCAL)
                        .oauthSubject(UUID.randomUUID().toString())
                        .active(true)
                        .build()
        );

        credentialRepository.save(
                LocalCredential.builder()
                        .user(user)
                        .email(normalizedEmail)
                        .passwordHash(passwordEncoder.encode(password))
                        .build()
        );

        return user;
    }

    @Transactional(readOnly = true)
    public User login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        LocalCredential credential = credentialRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(password, credential.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        return credential.getUser();
    }

    @Transactional
    public User seedAccount(String email, String password, String displayName) {
        String normalizedEmail = normalizeEmail(email);
        if (credentialRepository.existsByEmail(normalizedEmail)) {
            return credentialRepository.findByEmail(normalizedEmail).map(LocalCredential::getUser).orElseThrow();
        }

        User user = userRepository.save(
                User.builder()
                        .email(normalizedEmail)
                        .displayName(normalizeDisplayName(displayName, normalizedEmail))
                        .avatarUrl(null)
                        .oauthProvider(OauthProvider.LOCAL)
                        .oauthSubject(UUID.randomUUID().toString())
                        .active(true)
                        .build()
        );

        credentialRepository.save(
                LocalCredential.builder()
                        .user(user)
                        .email(normalizedEmail)
                        .passwordHash(passwordEncoder.encode(password))
                        .build()
        );

        return user;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Email is required");
        }
        return email.trim().toLowerCase();
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Password must be at least 8 characters long");
        }
    }

    private String normalizeDisplayName(String displayName, String fallbackEmail) {
        String name = displayName == null ? "" : displayName.trim();
        if (!name.isBlank()) {
            return name;
        }
        int at = fallbackEmail.indexOf('@');
        return at > 0 ? fallbackEmail.substring(0, at) : fallbackEmail;
    }
}
