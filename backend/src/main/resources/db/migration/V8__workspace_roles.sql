-- ─── workspace_roles ──────────────────────────────────────────────────────────
-- Defines roles that exist within a workspace. Default system roles (Admin,
-- Moderator, User) are created automatically when a workspace is created.
-- Admins can add custom roles (is_system = FALSE).

CREATE TABLE workspace_roles (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID        NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name         VARCHAR(50) NOT NULL,
    is_system    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_by   UUID        REFERENCES users(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_workspace_roles_ws_name UNIQUE (workspace_id, name)
);

CREATE INDEX idx_workspace_roles_workspace ON workspace_roles(workspace_id);

-- ─── workspace_member_roles ───────────────────────────────────────────────────
-- Many-to-many join between workspace_members and workspace_roles.

CREATE TABLE workspace_member_roles (
    member_id UUID NOT NULL REFERENCES workspace_members(id) ON DELETE CASCADE,
    role_id   UUID NOT NULL REFERENCES workspace_roles(id)   ON DELETE CASCADE,
    PRIMARY KEY (member_id, role_id)
);

-- ─── Data migration ────────────────────────────────────────────────────────────
-- 1. Create default system roles for every existing workspace.
INSERT INTO workspace_roles (workspace_id, name, is_system)
SELECT DISTINCT w.id, r.name, TRUE
FROM workspaces w
CROSS JOIN (VALUES ('Admin'), ('Moderator'), ('User')) AS r(name)
ON CONFLICT (workspace_id, name) DO NOTHING;

-- 2. Map existing workspace_members.role values into workspace_member_roles.
INSERT INTO workspace_member_roles (member_id, role_id)
SELECT wm.id, wr.id
FROM workspace_members wm
JOIN workspace_roles    wr ON wr.workspace_id = wm.workspace_id AND wr.name = wm.role
ON CONFLICT (member_id, role_id) DO NOTHING;

-- 3. Drop the old role column from workspace_members.
ALTER TABLE workspace_members DROP COLUMN IF EXISTS role;
