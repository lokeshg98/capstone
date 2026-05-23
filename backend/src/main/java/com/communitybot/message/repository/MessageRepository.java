package com.communitybot.message.repository;

import com.communitybot.message.domain.Message;
import com.communitybot.message.domain.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
            SELECT m FROM Message m
            WHERE m.channel.id = :channelId
              AND m.workspaceId = :wsId
              AND LOWER(m.body) LIKE LOWER(CONCAT('%', :needle, '%'))
              AND m.status NOT IN :excluded
            ORDER BY m.createdAt DESC
            """)
    List<Message> searchByChannelKeyword(
            @Param("channelId") UUID channelId,
            @Param("wsId") UUID workspaceId,
            @Param("needle") String needle,
            @Param("excluded") List<MessageStatus> excluded,
            Pageable pageable
    );

    /** Paginated top-level messages (no thread root), visible statuses only. */
    @Query("""
            SELECT m FROM Message m
            WHERE m.channel.id  = :channelId
              AND m.threadRoot  IS NULL
              AND m.status NOT IN :excludedStatuses
            ORDER BY m.createdAt ASC
            """)
    Page<Message> findTopLevel(UUID channelId, List<MessageStatus> excludedStatuses, Pageable pageable);

    /** Thread replies for a root message. */
    @Query("""
            SELECT m FROM Message m
            WHERE m.threadRoot.id = :rootId
              AND m.status NOT IN :excludedStatuses
            ORDER BY m.createdAt ASC
            """)
    List<Message> findThreadReplies(UUID rootId, List<MessageStatus> excludedStatuses);

    @Query("""
            SELECT m FROM Message m
            WHERE m.channel.id = :channelId
              AND m.createdAt >= :since
              AND m.createdAt < :until
              AND m.status NOT IN :excludedStatuses
            ORDER BY m.createdAt ASC
            """)
    List<Message> findByChannelIdAndCreatedAtBetween(
            @Param("channelId") UUID channelId,
            @Param("since") Instant since,
            @Param("until") Instant until,
            @Param("excludedStatuses") List<MessageStatus> excludedStatuses,
            Pageable pageable
    );

    @Query("""
            SELECT m.threadRoot.id, COUNT(m)
            FROM Message m
            WHERE m.threadRoot.id IN :rootIds
              AND m.status NOT IN :excludedStatuses
            GROUP BY m.threadRoot.id
            """)
    List<Object[]> countRepliesByRootIds(
            @Param("rootIds") List<UUID> rootIds,
            @Param("excludedStatuses") List<MessageStatus> excludedStatuses
    );
}
