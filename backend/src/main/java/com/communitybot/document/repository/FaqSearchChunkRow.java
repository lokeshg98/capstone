package com.communitybot.document.repository;

import java.util.UUID;

/** FAQ chunk row including human-readable document title for citations. */
public interface FaqSearchChunkRow {
    UUID   getId();
    String getChunkText();
    int    getChunkIndex();
    String getDocumentTitle();
}
