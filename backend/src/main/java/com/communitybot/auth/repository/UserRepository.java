package com.communitybot.auth.repository;

import com.communitybot.auth.domain.OauthProvider;
import com.communitybot.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByOauthProviderAndOauthSubject(OauthProvider provider, String subject);

    Optional<User> findByEmail(String email);
}
