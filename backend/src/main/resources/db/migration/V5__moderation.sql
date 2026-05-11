-- ─── Moderation Flags ────────────────────────────────────────────────────────
-- Tracks messages that were flagged by the LLM content classifier.
-- Status lifecycle: PENDING → APPROVED (false positive) | REMOVED (confirmed violation)

CREATE TABLE moderation_flags (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id      UUID        NOT NULL REFERENCES messages(id),
    workspace_id    UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    llm_reason      VARCHAR(50),
    llm_explanation TEXT,
    llm_confidence  DOUBLE PRECISION,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by     UUID        REFERENCES users(id),
    reviewed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_moderation_flags_workspace_status
    ON moderation_flags(workspace_id, status);

-- ─── Workspace Bans ──────────────────────────────────────────────────────────
-- A user with an active ban cannot send messages in the workspace.

CREATE TABLE workspace_bans (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id       UUID        NOT NULL REFERENCES users(id),
    banned_by     UUID        NOT NULL REFERENCES users(id),
    reason        TEXT,
    expires_at    TIMESTAMPTZ,            -- NULL = permanent
    active        BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workspace_bans_lookup
    ON workspace_bans(workspace_id, user_id, active);
