package com.communitybot.channel.repository;

import com.communitybot.channel.domain.Channel;
import com.communitybot.channel.domain.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    boolean existsByWorkspaceIdAndSlug(UUID workspaceId, String slug);

    Optional<Channel> findByWorkspaceIdAndSlug(UUID workspaceId, String slug);

    /** Public channels the user can see in this workspace. */
    List<Channel> findAllByWorkspaceIdAndTypeOrderByNameAsc(UUID workspaceId, ChannelType type);

    /** All channels (public + private) the user is explicitly a member of. */
    @Query("""
            SELECT cm.channel
            FROM ChannelMember cm
            WHERE cm.user.id  = :userId
              AND cm.channel.workspace.id = :workspaceId
            ORDER BY cm.channel.name
            """)
    List<Channel> findJoinedByUserIdAndWorkspaceId(UUID userId, UUID workspaceId);
}
