package com.communitybot.document.domain;

import com.communitybot.attachment.domain.Attachment;
import com.communitybot.shared.domain.BaseEntity;
import com.communitybot.workspace.domain.Workspace;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "faq_documents")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FaqDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    /** The original attachment this document was ingested from. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id", nullable = false)
    private Attachment attachment;

    /** Human-readable title, defaults to the attachment filename. */
    @Column(nullable = false)
    private String title;

    /** Number of chunks produced during ingestion; recorded for observability. */
    @Column(name = "chunk_count")
    private Integer chunkCount;

    public void recordChunkCount(int count) {
        this.chunkCount = count;
    }
}
