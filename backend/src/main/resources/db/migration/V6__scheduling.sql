-- Scheduled posts ----------------------------------------------------------
CREATE TABLE scheduled_posts (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id    UUID            NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    channel_id      UUID            NOT NULL REFERENCES channels(id)   ON DELETE CASCADE,
    created_by      UUID            NOT NULL REFERENCES users(id),
    body            TEXT            NOT NULL,
    schedule_type   VARCHAR(20)     NOT NULL,          -- ONE_SHOT | CRON
    cron_expression TEXT,
    next_fire_at    TIMESTAMPTZ     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    last_sent_at    TIMESTAMPTZ,
    error_message   TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_scheduled_posts_due
    ON scheduled_posts (status, next_fire_at)
    WHERE status = 'PENDING';

-- Welcome message template per workspace ------------------------------------
ALTER TABLE workspaces
    ADD COLUMN welcome_message_template TEXT;
