-- Per-turn Ask Bot conversation vectors (user + assistant messages, scoped by conversation)

ALTER TABLE agent_memories
    ADD COLUMN IF NOT EXISTS conversation_id UUID,
    ADD COLUMN IF NOT EXISTS role VARCHAR(16);

CREATE INDEX IF NOT EXISTS idx_agent_memories_conversation
    ON agent_memories(conversation_id, created_at DESC)
    WHERE conversation_id IS NOT NULL;

COMMENT ON COLUMN agent_memories.conversation_id IS 'Ask Bot session id from the client; groups turns in one chat';
COMMENT ON COLUMN agent_memories.role IS 'USER, ASSISTANT, or legacy TURN_SUMMARY';
