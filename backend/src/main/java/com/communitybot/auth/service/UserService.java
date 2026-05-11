package com.communitybot.auth.service;

import com.communitybot.auth.domain.User;
import com.communitybot.auth.repository.UserRepository;
import com.communitybot.shared.exception.AppException;
import com.communitybot.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Loads a {@link UserDetails} by user ID. Called by {@link com.communitybot.auth.config.JwtAuthFilter}
     * on every authenticated request.
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID id) {
        User user = getOrThrow(id);
        return new org.springframework.security.core.userdetails.User(
                user.getId().toString(),
                "",   // no password — OAuth2 only
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Transactional(readOnly = true)
    public User getOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
