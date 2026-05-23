package com.communitybot.scheduling.repository;

import com.communitybot.scheduling.domain.UserDigestPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserDigestPreferencesRepository extends JpaRepository<UserDigestPreferences, UUID> {}
