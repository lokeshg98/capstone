-- Add ON DELETE CASCADE to moderation_flags.message_id FK

ALTER TABLE moderation_flags DROP CONSTRAINT IF EXISTS moderation_flags_message_id_fkey;

ALTER TABLE moderation_flags
    ADD CONSTRAINT fk_moderation_flags_message
        FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE;
