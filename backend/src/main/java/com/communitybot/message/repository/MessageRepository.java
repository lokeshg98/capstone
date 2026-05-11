package com.communitybot.message.repository;

import com.communitybot.message.domain.Message;
import com.communitybot.message.domain.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

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
}
