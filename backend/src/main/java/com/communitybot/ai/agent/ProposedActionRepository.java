package com.communitybot.ai.agent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProposedActionRepository extends JpaRepository<ProposedAction, UUID> {

    Optional<ProposedAction> findByIdAndWorkspaceIdAndUserIdAndStatus(
            UUID id, UUID workspaceId, UUID userId, ProposedAction.Status status);
}
