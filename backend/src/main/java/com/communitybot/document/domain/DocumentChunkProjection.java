package com.communitybot.document.domain;

import java.util.UUID;

/**
 * Spring Data projection returned by the native cosine-similarity query.
 * Intentionally omits the {@code embedding} column to avoid large binary reads.
 */
public interface DocumentChunkProjection {
    UUID   getId();
    String getChunkText();
    int    getChunkIndex();
}
