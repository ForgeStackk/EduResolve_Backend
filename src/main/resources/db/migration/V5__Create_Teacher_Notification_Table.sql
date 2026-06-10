CREATE TABLE IF NOT EXISTS teacher_notification (
    notification_id UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id      UUID        NOT NULL,
    message         TEXT        NOT NULL,
    is_read         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tn_teacher_id   ON teacher_notification(teacher_id);
CREATE INDEX IF NOT EXISTS idx_tn_read_status  ON teacher_notification(teacher_id, is_read);
