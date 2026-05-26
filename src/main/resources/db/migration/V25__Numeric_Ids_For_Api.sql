-- Add numeric sequence IDs to classroom and student tables for API use.
-- UUIDs remain the internal JPA primary keys; seq_id is exposed to clients.

ALTER TABLE classroom ADD COLUMN IF NOT EXISTS seq_id BIGSERIAL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_classroom_seq_id ON classroom(seq_id);

ALTER TABLE tp_student ADD COLUMN IF NOT EXISTS seq_id BIGSERIAL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_tp_student_seq_id ON tp_student(seq_id);

-- Migrate at_risk_snapshots: replace UUID student_id with numeric student_seq_id.
ALTER TABLE at_risk_snapshots ADD COLUMN IF NOT EXISTS student_seq_id BIGINT;

UPDATE at_risk_snapshots ars
SET student_seq_id = s.seq_id
FROM tp_student s
WHERE s.student_id = ars.student_id;

ALTER TABLE at_risk_snapshots DROP CONSTRAINT IF EXISTS uq_at_risk_student_date;
ALTER TABLE at_risk_snapshots DROP COLUMN IF EXISTS student_id;
ALTER TABLE at_risk_snapshots ALTER COLUMN student_seq_id SET NOT NULL;
ALTER TABLE at_risk_snapshots ADD CONSTRAINT uq_at_risk_student_date
    UNIQUE (student_seq_id, snapshot_date);
