-- Denormalize student identity onto doubt_thread for fast teacher-side queries.
ALTER TABLE doubt_thread
    ADD COLUMN IF NOT EXISTS student_name    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS student_class   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS student_section VARCHAR(10);

-- Backfill existing rows from tp_student → classroom
UPDATE doubt_thread dt
SET student_name    = COALESCE(s.full_name, ''),
    student_class   = COALESCE(cr.class_name, ''),
    student_section = COALESCE(cr.section, '')
FROM tp_student s
         JOIN classroom cr ON cr.class_id = s.class_id
WHERE s.user_id = dt.student_id
  AND dt.student_name IS NULL;

-- Composite index for teacher class-filter query: (teacher_id, student_class, created_at DESC)
CREATE INDEX IF NOT EXISTS idx_doubt_thread_teacher_class
    ON doubt_thread (teacher_id, student_class, created_at DESC);
