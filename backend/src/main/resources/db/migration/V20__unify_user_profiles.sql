-- Unify user profile data into the user_profiles table and remove duplicate
-- columns from the users table (merged from two competing implementations).

-- Add status_message to user_profiles (was only on users table previously)
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS status_message VARCHAR(200);

-- Migrate profile data from users columns into user_profiles
INSERT INTO user_profiles (user_id, about_me, interests, status_message, show_email, show_phone, notification_mode, updated_at)
SELECT
    id,
    about_me,
    interests,
    status_message,
    false,
    false,
    'ALL',
    NOW()
FROM users
WHERE (about_me IS NOT NULL OR interests IS NOT NULL OR status_message IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM user_profiles WHERE user_profiles.user_id = users.id)
ON CONFLICT (user_id) DO UPDATE SET
    about_me       = COALESCE(user_profiles.about_me,       users.about_me),
    interests      = COALESCE(user_profiles.interests,      users.interests),
    status_message = COALESCE(user_profiles.status_message, users.status_message);

-- Drop the profile columns that now live in user_profiles
ALTER TABLE users DROP COLUMN IF EXISTS status_message;
ALTER TABLE users DROP COLUMN IF EXISTS about_me;
ALTER TABLE users DROP COLUMN IF EXISTS interests;
ALTER TABLE users DROP COLUMN IF EXISTS contact_info;
