-- ─── FAQ Documents ───────────────────────────────────────────────────────────
-- Tracks each file that has been ingested into the vector store.

CREATE TABLE faq_documents (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  UUID         NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    attachment_id UUID         NOT NULL REFERENCES attachments(id),
    title         VARCHAR(255) NOT NULL,
    chunk_count   INTEGER,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_faq_documents_workspace ON faq_documents(workspace_id);

-- ─── Document Chunks ──────────────────────────────────────────────────────────
-- Stores text windows and their pgvector embeddings for cosine similarity search.
-- The embedding column is written via raw JDBC (::vector cast) in the application.

CREATE TABLE document_chunks (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    faq_document_id  UUID        NOT NULL REFERENCES faq_documents(id) ON DELETE CASCADE,
    workspace_id     UUID        NOT NULL REFERENCES workspaces(id)    ON DELETE CASCADE,
    chunk_text       TEXT        NOT NULL,
    chunk_index      INTEGER     NOT NULL,
    embedding        vector(1536),          -- null until the application writes it
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- IVFFlat index for approximate nearest-neighbour search (cosine distance).
-- lists=100 is appropriate for up to ~1M vectors; rebuild with more lists as data grows.
CREATE INDEX idx_document_chunks_embedding
    ON document_chunks USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

CREATE INDEX idx_document_chunks_workspace ON document_chunks(workspace_id);
