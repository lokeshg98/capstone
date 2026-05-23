-- Vector index over message bodies for semantic channel search

CREATE TABLE message_embeddings (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id   UUID        NOT NULL UNIQUE REFERENCES messages(id) ON DELETE CASCADE,
    workspace_id UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    embedding    vector(1536) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_message_embeddings_workspace ON message_embeddings(workspace_id);
CREATE INDEX idx_message_embeddings_vec
    ON message_embeddings USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
