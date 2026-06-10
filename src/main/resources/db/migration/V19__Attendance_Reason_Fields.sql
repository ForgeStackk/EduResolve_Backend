-- Add reason_code, audit fields to attendance; add missing indexes
ALTER TABLE attendance
    ADD COLUMN IF NOT EXISTS reason_code         VARCHAR(20),
    ADD COLUMN IF NOT EXISTS last_edited_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS last_edited_at      TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_att_class_date   ON attendance(class_id, date);
CREATE INDEX IF NOT EXISTS idx_att_student_date ON attendance(student_id, date);
