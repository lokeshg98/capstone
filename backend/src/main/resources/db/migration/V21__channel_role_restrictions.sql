-- Add role-based channel access control.
-- Channels remain open to all workspace members by default;
-- when role_restricted = true, only members with one of the listed roles can see/join.

ALTER TABLE channels ADD COLUMN IF NOT EXISTS role_restricted BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS channel_accessible_roles (
    channel_id UUID NOT NULL REFERENCES channels(id) ON DELETE CASCADE,
    role_name  VARCHAR(191) NOT NULL,
    PRIMARY KEY (channel_id, role_name)
);
