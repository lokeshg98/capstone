package com.communitybot.channel.domain;

import com.communitybot.auth.domain.User;
import com.communitybot.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "channel_members",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_channel_members",
        columnNames = {"channel_id", "user_id"}
    )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChannelMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Tracks which messages the user has already seen — drives unread counts. */
    @Column(name = "last_read_at")
    private Instant lastReadAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean muted = false;

    public void markRead(Instant at) { this.lastReadAt = at; }
    public void toggleMute(boolean muted) { this.muted = muted; }
}
