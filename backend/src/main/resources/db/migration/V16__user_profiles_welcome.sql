-- Extended user profile for personalized onboarding and member discovery

CREATE TABLE user_profiles (
    user_id              UUID         PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    about_me             TEXT,
    phone                VARCHAR(32),
    show_email           BOOLEAN      NOT NULL DEFAULT FALSE,
    show_phone           BOOLEAN      NOT NULL DEFAULT FALSE,
    interests            TEXT,
    notification_mode    VARCHAR(32)  NOT NULL DEFAULT 'ALL',
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Sensible default welcome template for new workspaces (personalized by bot on join)
UPDATE workspaces
SET welcome_message_template = COALESCE(
    welcome_message_template,
    '👋 Welcome to the community, {name}! We''re glad you''re here. Introduce yourself in #general and explore the channels.'
)
WHERE welcome_message_template IS NULL OR TRIM(welcome_message_template) = '';
