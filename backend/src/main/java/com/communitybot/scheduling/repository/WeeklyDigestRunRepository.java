package com.communitybot.scheduling.repository;

import com.communitybot.scheduling.domain.WeeklyDigestRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface WeeklyDigestRunRepository extends JpaRepository<WeeklyDigestRun, UUID> {

    boolean existsByWorkspaceIdAndUserIdAndPeriodStartAndPeriodEnd(
            UUID workspaceId, UUID userId, Instant periodStart, Instant periodEnd);
}
