package com.communitybot.message.domain;

import com.communitybot.attachment.domain.Attachment;
import com.communitybot.auth.domain.User;
import com.communitybot.channel.domain.Channel;
import com.communitybot.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    /**
     * Denormalised for efficient workspace-scoped queries without joining through channels.
     */
    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    /** Points to the first message in a thread; null for top-level messages. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_root_id")
    private Message threadRoot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessageStatus status = MessageStatus.ACTIVE;

    @Column(name = "edited_at")
    private Instant editedAt;

    /** Optional file attachment. Set once at creation; never mutated. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id")
    private Attachment attachment;

    public void edit(String newBody) {
        this.body     = newBody;
        this.status   = MessageStatus.EDITED;
        this.editedAt = Instant.now();
    }

    public void hide()    { this.status = MessageStatus.HIDDEN; }
    public void flag()    { this.status = MessageStatus.FLAGGED; }
    public void delete()  { this.status = MessageStatus.DELETED; }
    public void restore() { this.status = MessageStatus.ACTIVE; }
}
