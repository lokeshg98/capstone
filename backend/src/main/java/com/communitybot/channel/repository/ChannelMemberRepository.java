package com.communitybot.channel.repository;

import com.communitybot.channel.domain.ChannelMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelMemberRepository extends JpaRepository<ChannelMember, UUID> {

    Optional<ChannelMember> findByChannelIdAndUserId(UUID channelId, UUID userId);

    boolean existsByChannelIdAndUserId(UUID channelId, UUID userId);

    List<ChannelMember> findAllByChannelId(UUID channelId);

    @Query("""
            SELECT cm FROM ChannelMember cm
            JOIN FETCH cm.user
            WHERE cm.channel.id = :channelId
            ORDER BY cm.createdAt ASC
            """)
    List<ChannelMember> findAllByChannelIdWithUser(@Param("channelId") UUID channelId);

    @Query("""
            SELECT cm FROM ChannelMember cm
            JOIN FETCH cm.channel
            WHERE cm.user.id = :userId
              AND cm.channel.workspace.id = :workspaceId
              AND cm.muted = false
            ORDER BY cm.channel.name
            """)
    List<ChannelMember> findActiveMemberships(
            @Param("userId") UUID userId,
            @Param("workspaceId") UUID workspaceId
    );
}
