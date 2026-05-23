-- ─── User profile extensions ──────────────────────────────────────────────────
-- Adds optional profile fields to the users table for the profile badge feature.

ALTER TABLE users ADD COLUMN IF NOT EXISTS status_message VARCHAR(200);
ALTER TABLE users ADD COLUMN IF NOT EXISTS about_me       TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS interests      TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS contact_info   TEXT;
