package com.communitybot.document.domain;

import com.communitybot.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Stores one text chunk from a FAQ document.
 * The {@code embedding} column (type {@code vector(1536)}) is NOT mapped via JPA —
 * it is written by {@link com.communitybot.document.service.DocumentIngestionService}
 * using a raw JDBC UPDATE so pgvector's {@code ::vector} cast is applied correctly.
 * Similarity queries use native SQL and return {@link DocumentChunkProjection}.
 */
@Entity
@Table(name = "document_chunks")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DocumentChunk extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faq_document_id", nullable = false)
    private FaqDocument faqDocument;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "chunk_text", columnDefinition = "TEXT", nullable = false)
    private String chunkText;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;
}
