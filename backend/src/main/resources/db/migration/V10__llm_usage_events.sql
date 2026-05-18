-- Token usage & estimated cost for OpenAI calls (chat, embeddings, classifier).

CREATE TABLE llm_usage_events (
    id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID           NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category      VARCHAR(32)    NOT NULL,
    model         VARCHAR(191)   NOT NULL,
    input_tokens  INTEGER        NOT NULL DEFAULT 0 CHECK (input_tokens >= 0),
    output_tokens INTEGER        NOT NULL DEFAULT 0 CHECK (output_tokens >= 0),
    cost_usd      NUMERIC(14, 6) NOT NULL DEFAULT 0 CHECK (cost_usd >= 0),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_llm_usage_events_user ON llm_usage_events(user_id);
CREATE INDEX idx_llm_usage_events_created ON llm_usage_events(created_at);
