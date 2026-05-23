package com.communitybot.auth.repository;

import com.communitybot.auth.domain.LocalCredential;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LocalCredentialRepository extends JpaRepository<LocalCredential, UUID> {

    @EntityGraph(attributePaths = "user")
    Optional<LocalCredential> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<LocalCredential> findByUserId(UUID userId);
}
