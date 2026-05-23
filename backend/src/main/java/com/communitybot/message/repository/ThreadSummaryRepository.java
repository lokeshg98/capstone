package com.communitybot.message.repository;

import com.communitybot.message.domain.ThreadSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ThreadSummaryRepository extends JpaRepository<ThreadSummary, UUID> {

    boolean existsByThreadRootId(UUID threadRootId);

    Optional<ThreadSummary> findByThreadRootId(UUID threadRootId);
}
