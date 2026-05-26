-- Allow admin broadcasts to fan-out to parent_inbox without a message FK
ALTER TABLE parent_inbox
    ALTER COLUMN message_id DROP NOT NULL,
    ADD COLUMN IF NOT EXISTS broadcast_id BIGINT REFERENCES broadcast(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_pi_broadcast_id ON parent_inbox(broadcast_id) WHERE broadcast_id IS NOT NULL;
