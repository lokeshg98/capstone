-- Add ON DELETE CASCADE to messages.workspace_id FK (matches all other workspace FKs)

ALTER TABLE messages DROP CONSTRAINT IF EXISTS messages_workspace_id_fkey;

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;
