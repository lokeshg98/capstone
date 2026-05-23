-- Per-user weekly digest preferences (n8n-triggered digests across subscribed channels)

CREATE TABLE user_digest_preferences (
    user_id                 UUID         PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    weekly_digest_enabled   BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Idempotency: one digest per user per workspace per period
CREATE TABLE weekly_digest_runs (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id    UUID         NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    period_start    TIMESTAMPTZ  NOT NULL,
    period_end      TIMESTAMPTZ  NOT NULL,
    channel_id      UUID         REFERENCES channels(id) ON DELETE CASCADE,
    message_id      UUID         REFERENCES messages(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_weekly_digest_run UNIQUE (workspace_id, user_id, period_start, period_end)
);

CREATE INDEX idx_weekly_digest_runs_workspace ON weekly_digest_runs(workspace_id, period_end DESC);
