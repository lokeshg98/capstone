package com.communitybot.message.repository;

import com.communitybot.message.domain.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReactionRepository extends JpaRepository<Reaction, UUID> {

    /** Bulk-load reactions for a batch of messages — avoids N+1 when rendering a page. */
    @Query("SELECT r FROM Reaction r WHERE r.message.id IN :messageIds")
    List<Reaction> findAllByMessageIdIn(List<UUID> messageIds);

    Optional<Reaction> findByMessageIdAndUserIdAndEmoji(UUID messageId, UUID userId, String emoji);

    boolean existsByMessageIdAndUserIdAndEmoji(UUID messageId, UUID userId, String emoji);

    void deleteByMessageIdAndUserIdAndEmoji(UUID messageId, UUID userId, String emoji);
}
