-- ─── channels ─────────────────────────────────────────────────────────────────
CREATE TABLE channels (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID         NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name         VARCHAR(100) NOT NULL,
    slug         VARCHAR(100) NOT NULL,
    type         VARCHAR(20)  NOT NULL DEFAULT 'PUBLIC',
    description  TEXT,
    created_by   UUID         NOT NULL REFERENCES users(id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_channels_ws_slug UNIQUE (workspace_id, slug)
);

CREATE INDEX idx_channels_workspace ON channels(workspace_id);

-- ─── channel_members ──────────────────────────────────────────────────────────
CREATE TABLE channel_members (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id   UUID        NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    user_id      UUID        NOT NULL REFERENCES users(id),
    last_read_at TIMESTAMPTZ,
    muted        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_channel_members UNIQUE (channel_id, user_id)
);

CREATE INDEX idx_channel_members_user ON channel_members(user_id);

-- ─── messages ─────────────────────────────────────────────────────────────────
CREATE TABLE messages (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id     UUID        NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    workspace_id   UUID        NOT NULL REFERENCES workspaces(id),
    author_id      UUID        NOT NULL REFERENCES users(id),
    body           TEXT        NOT NULL,
    thread_root_id UUID        REFERENCES messages(id),
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    edited_at      TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Primary query pattern: fetch a channel's top-level messages ordered by time
CREATE INDEX idx_messages_channel_created ON messages(channel_id, created_at ASC)
    WHERE thread_root_id IS NULL;

-- Thread reply lookup
CREATE INDEX idx_messages_thread_root ON messages(thread_root_id)
    WHERE thread_root_id IS NOT NULL;

-- ─── reactions ────────────────────────────────────────────────────────────────
CREATE TABLE reactions (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID        NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users(id),
    emoji      VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reactions UNIQUE (message_id, user_id, emoji)
);

CREATE INDEX idx_reactions_message ON reactions(message_id);
