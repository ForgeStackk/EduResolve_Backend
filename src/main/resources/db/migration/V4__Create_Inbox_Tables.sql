-- Student inbox: one row per (student, message) delivery
CREATE TABLE IF NOT EXISTS student_inbox (
    inbox_id    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id  UUID        NOT NULL,
    message_id  UUID        NOT NULL,
    read_status VARCHAR(10) NOT NULL DEFAULT 'UNREAD',
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_si_message FOREIGN KEY (message_id)
        REFERENCES message(message_id) ON DELETE CASCADE
);

-- Parent inbox: one row per (parent, message) delivery
CREATE TABLE IF NOT EXISTS parent_inbox (
    inbox_id    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id   UUID        NOT NULL,
    message_id  UUID        NOT NULL,
    read_status VARCHAR(10) NOT NULL DEFAULT 'UNREAD',
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_pi_message FOREIGN KEY (message_id)
        REFERENCES message(message_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_si_student_id     ON student_inbox(student_id);
CREATE INDEX IF NOT EXISTS idx_si_student_status ON student_inbox(student_id, read_status);
CREATE INDEX IF NOT EXISTS idx_pi_parent_id      ON parent_inbox(parent_id);
CREATE INDEX IF NOT EXISTS idx_pi_parent_status  ON parent_inbox(parent_id, read_status);
