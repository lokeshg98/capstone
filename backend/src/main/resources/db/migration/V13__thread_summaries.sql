-- Otter-style auto-generated digests for long discussion threads (10+ messages)

CREATE TABLE thread_summaries (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_root_id   UUID         NOT NULL UNIQUE REFERENCES messages(id) ON DELETE CASCADE,
    workspace_id     UUID         NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    channel_id       UUID         NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    message_count    INTEGER      NOT NULL CHECK (message_count >= 1),
    summary_body     TEXT         NOT NULL,
    bot_message_id   UUID         REFERENCES messages(id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_thread_summaries_workspace ON thread_summaries(workspace_id);
