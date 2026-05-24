package com.communitybot.document.repository;

import com.communitybot.document.domain.DocumentChunk;
import com.communitybot.document.domain.DocumentChunkProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    @Modifying
    @Query("DELETE FROM DocumentChunk dc WHERE dc.faqDocument.id = :faqDocId")
    void deleteAllByFaqDocumentId(@Param("faqDocId") UUID faqDocId);

    /**
     * Returns the top-5 chunks closest to the query vector using pgvector cosine distance.
     * The {@code :queryVector} parameter must be a string in pgvector format, e.g. {@code "[0.1,0.2,...]"}.
     */
    @Query(value = """
            SELECT id, workspace_id, faq_document_id, chunk_text, chunk_index, created_at, updated_at
            FROM document_chunks
            WHERE workspace_id = :wsId
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT 5
            """, nativeQuery = true)
    List<DocumentChunkProjection> findTopKByWorkspace(
            @Param("wsId") UUID wsId,
            @Param("queryVector") String queryVector
    );

    @Query(value = """
            SELECT dc.id, dc.chunk_text, dc.chunk_index, fd.title AS document_title
            FROM document_chunks dc
            JOIN faq_documents fd ON fd.id = dc.faq_document_id
            WHERE dc.workspace_id = :wsId
            ORDER BY dc.embedding <=> CAST(:queryVector AS vector)
            LIMIT 5
            """, nativeQuery = true)
    List<FaqSearchChunkRow> findTopFaqChunksWithTitles(
            @Param("wsId") UUID wsId,
            @Param("queryVector") String queryVector
    );
}
