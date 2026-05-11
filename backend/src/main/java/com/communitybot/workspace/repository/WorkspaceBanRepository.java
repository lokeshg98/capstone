package com.communitybot.workspace.repository;

import com.communitybot.workspace.domain.WorkspaceBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceBanRepository extends JpaRepository<WorkspaceBan, UUID> {

    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM WorkspaceBan b
            WHERE b.workspace.id = :wsId
              AND b.user.id      = :userId
              AND b.active       = true
              AND (b.expiresAt IS NULL OR b.expiresAt > CURRENT_TIMESTAMP)
            """)
    boolean isUserBanned(@Param("wsId") UUID wsId, @Param("userId") UUID userId);

    @Query("""
            SELECT b FROM WorkspaceBan b
            WHERE b.workspace.id = :wsId
              AND b.user.id      = :userId
              AND b.active       = true
              AND (b.expiresAt IS NULL OR b.expiresAt > CURRENT_TIMESTAMP)
            """)
    Optional<WorkspaceBan> findActiveBan(@Param("wsId") UUID wsId, @Param("userId") UUID userId);

    List<WorkspaceBan> findAllByWorkspaceIdAndActiveTrueOrderByCreatedAtDesc(UUID workspaceId);
}
