package com.communitybot.message.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "thread_summaries")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ThreadSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "thread_root_id", nullable = false, unique = true)
    private UUID threadRootId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(name = "message_count", nullable = false)
    private int messageCount;

    @Column(name = "summary_body", nullable = false, columnDefinition = "TEXT")
    private String summaryBody;

    @Column(name = "bot_message_id")
    private UUID botMessageId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
