package com.communitybot.scheduling.repository;

import com.communitybot.scheduling.domain.ScheduledPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ScheduledPostRepository extends JpaRepository<ScheduledPost, UUID> {

    List<ScheduledPost> findAllByWorkspaceIdOrderByNextFireAtAsc(UUID workspaceId);

    /**
     * Locks up to 50 due rows with SKIP LOCKED so concurrent instances never
     * double-deliver the same post.
     */
    @Query(value = """
            SELECT * FROM scheduled_posts
            WHERE status = 'PENDING' AND next_fire_at <= :now
            ORDER BY next_fire_at ASC
            LIMIT 50
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<ScheduledPost> findDuePostsForUpdate(@Param("now") Instant now);
}
