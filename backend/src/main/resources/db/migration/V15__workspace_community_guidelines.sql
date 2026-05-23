-- Per-workspace community guidelines used alongside OpenAI Moderation API

ALTER TABLE workspaces
    ADD COLUMN community_guidelines TEXT;
