-- Long-term semantic memory for multi-agent Ask Bot (per workspace + user)

CREATE TABLE agent_memories (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID         NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind         VARCHAR(32)  NOT NULL DEFAULT 'TURN_SUMMARY',
    content      TEXT         NOT NULL,
    embedding    vector(1536),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_memories_workspace_user ON agent_memories(workspace_id, user_id);
CREATE INDEX idx_agent_memories_embedding
    ON agent_memories USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
