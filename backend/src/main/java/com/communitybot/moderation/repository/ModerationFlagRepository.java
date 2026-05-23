package com.communitybot.moderation.repository;

import com.communitybot.moderation.domain.FlagStatus;
import com.communitybot.moderation.domain.ModerationFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ModerationFlagRepository extends JpaRepository<ModerationFlag, UUID> {

    List<ModerationFlag> findAllByWorkspaceIdAndStatusOrderByCreatedAtDesc(
            UUID workspaceId, FlagStatus status);

    List<ModerationFlag> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    boolean existsByMessageId(UUID messageId);

    long countByWorkspaceIdAndStatus(UUID workspaceId, FlagStatus status);

    long countByWorkspaceIdAndCreatedAtGreaterThanEqual(UUID workspaceId, java.time.Instant since);
}
