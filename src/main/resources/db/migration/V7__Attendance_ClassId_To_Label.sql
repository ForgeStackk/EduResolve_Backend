-- Replace UUID class_id with human-readable label (e.g. "9A") in attendance tables.
-- Orphaned rows (UUID not in classroom) are deleted first to avoid null violations.

-- ── attendance ──────────────────────────────────────────────────────────────

DELETE FROM attendance WHERE class_id NOT IN (SELECT class_id FROM classroom);

ALTER TABLE attendance ADD COLUMN class_label VARCHAR(20);

UPDATE attendance a
SET class_label = REPLACE(c.class_name, 'Class ', '') || c.section
FROM classroom c
WHERE c.class_id = a.class_id;

-- CASCADE drops the FK constraint, unique constraint, and idx_att_class_date
ALTER TABLE attendance DROP COLUMN class_id CASCADE;
ALTER TABLE attendance RENAME COLUMN class_label TO class_id;
ALTER TABLE attendance ALTER COLUMN class_id SET NOT NULL;

ALTER TABLE attendance
    ADD CONSTRAINT uq_attendance_class_student_date UNIQUE (class_id, student_id, date);
CREATE INDEX idx_att_class_date ON attendance(class_id, date);

-- ── attendance_report ───────────────────────────────────────────────────────

DELETE FROM attendance_report WHERE class_id NOT IN (SELECT class_id FROM classroom);

ALTER TABLE attendance_report ADD COLUMN class_label VARCHAR(20);

UPDATE attendance_report r
SET class_label = REPLACE(c.class_name, 'Class ', '') || c.section
FROM classroom c
WHERE c.class_id = r.class_id;

ALTER TABLE attendance_report DROP COLUMN class_id CASCADE;
ALTER TABLE attendance_report RENAME COLUMN class_label TO class_id;
ALTER TABLE attendance_report ALTER COLUMN class_id SET NOT NULL;

CREATE INDEX idx_ar_class_period ON attendance_report(class_id, year DESC, month DESC);
