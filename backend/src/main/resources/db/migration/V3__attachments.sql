-- ─── Attachments ─────────────────────────────────────────────────────────────
-- Stores metadata for uploaded files. The actual bytes live in MinIO.
-- Only clean (AV-passed) files reach this table; infected files are rejected
-- before the record is ever created.

CREATE TABLE attachments (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    uploaded_by  UUID         NOT NULL REFERENCES users(id),
    filename     VARCHAR(255) NOT NULL,
    mime_type    VARCHAR(127) NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    kind         VARCHAR(10)  NOT NULL,      -- PDF | DOCX
    storage_key  TEXT         NOT NULL,      -- MinIO object key
    scan_status  VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ─── Link messages to their optional attachment ───────────────────────────────

ALTER TABLE messages
    ADD COLUMN attachment_id UUID REFERENCES attachments(id);
