-- Human-in-the-loop confirmations for agent-proposed write actions (e.g. scheduled posts)

CREATE TABLE proposed_actions (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID         NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action_type  VARCHAR(64)  NOT NULL,
    payload      JSONB        NOT NULL,
    status       VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_proposed_actions_workspace_user_status
    ON proposed_actions(workspace_id, user_id, status);
