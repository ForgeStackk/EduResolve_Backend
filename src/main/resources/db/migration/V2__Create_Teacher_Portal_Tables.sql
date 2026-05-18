-- Teacher Portal Schema
-- V2__Create_Teacher_Portal_Tables.sql
--
-- Naming notes:
--   • teacher / classroom   → new teacher-portal entities (not the legacy teacher_profile)
--   • tp_student / tp_parent → prefixed to avoid collision with legacy student_profile / parents_profile
--   • gen_random_uuid()     → built-in from PostgreSQL 13+ (no extension needed)

-- =========================================================
-- 1. teacher (no FK to classroom yet — circular dep)
-- =========================================================
CREATE TABLE IF NOT EXISTS teacher (
    teacher_id       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name        VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL UNIQUE,
    phone            VARCHAR(20),
    class_teacher_of UUID,                        -- FK added below after classroom exists
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
);

-- =========================================================
-- 2. classroom (no FK to teacher yet — circular dep)
-- =========================================================
CREATE TABLE IF NOT EXISTS classroom (
    class_id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    class_name       VARCHAR(100) NOT NULL,
    section          VARCHAR(10)  NOT NULL,
    academic_year    VARCHAR(20)  NOT NULL,
    class_teacher_id UUID                         -- FK added below after teacher exists
);

-- =========================================================
-- 3. Resolve circular FKs now that both tables exist
-- =========================================================
DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_teacher_class_teacher_of'
    ) THEN
        ALTER TABLE teacher
            ADD CONSTRAINT fk_teacher_class_teacher_of
            FOREIGN KEY (class_teacher_of) REFERENCES classroom(class_id) ON DELETE SET NULL;
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_classroom_class_teacher'
    ) THEN
        ALTER TABLE classroom
            ADD CONSTRAINT fk_classroom_class_teacher
            FOREIGN KEY (class_teacher_id) REFERENCES teacher(teacher_id) ON DELETE SET NULL;
    END IF;
END $$;

-- =========================================================
-- 4. teacher_subject_mapping
--    Answers: which classes & subjects is this teacher assigned to?
-- =========================================================
CREATE TABLE IF NOT EXISTS teacher_subject_mapping (
    id         UUID   PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id UUID   NOT NULL REFERENCES teacher(teacher_id)   ON DELETE CASCADE,
    class_id   UUID   NOT NULL REFERENCES classroom(class_id)   ON DELETE CASCADE,
    subject_id BIGINT NOT NULL                                    -- FK → existing subjects table (BIGSERIAL)
);

-- =========================================================
-- 5. tp_parent (must come before tp_student for FK)
-- =========================================================
CREATE TABLE IF NOT EXISTS tp_parent (
    parent_id UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    email     VARCHAR(255) NOT NULL,
    phone     VARCHAR(20)  NOT NULL
);

-- =========================================================
-- 6. tp_student
-- =========================================================
CREATE TABLE IF NOT EXISTS tp_student (
    student_id  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name   VARCHAR(255) NOT NULL,
    roll_number VARCHAR(50)  NOT NULL,
    class_id    UUID         NOT NULL REFERENCES classroom(class_id) ON DELETE RESTRICT,
    parent_id   UUID         REFERENCES tp_parent(parent_id) ON DELETE SET NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
);

-- =========================================================
-- 7. attendance
--    UNIQUE on (class_id, student_id, date) + @Version via version column
-- =========================================================
CREATE TABLE IF NOT EXISTS attendance (
    attendance_id UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id      UUID        NOT NULL REFERENCES classroom(class_id)  ON DELETE RESTRICT,
    student_id    UUID        NOT NULL REFERENCES tp_student(student_id) ON DELETE RESTRICT,
    date          DATE        NOT NULL,
    status        VARCHAR(20) NOT NULL,
    marked_by     UUID        NOT NULL REFERENCES teacher(teacher_id)   ON DELETE RESTRICT,
    marked_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remarks       TEXT,
    version       BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_attendance_class_student_date UNIQUE (class_id, student_id, date)
);

-- =========================================================
-- 8. message
-- =========================================================
CREATE TABLE IF NOT EXISTS message (
    message_id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id         UUID        NOT NULL,
    sender_role       VARCHAR(20) NOT NULL DEFAULT 'TEACHER',
    recipient_type    VARCHAR(50) NOT NULL,
    target_class_id   UUID        REFERENCES classroom(class_id) ON DELETE SET NULL,
    target_subject_id BIGINT,
    content_type      VARCHAR(20) NOT NULL,
    text_body         TEXT,
    sent_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_homework       BOOLEAN     NOT NULL DEFAULT FALSE,
    homework_due_date DATE
);

-- =========================================================
-- 9. message_attachment
-- =========================================================
CREATE TABLE IF NOT EXISTS message_attachment (
    attachment_id   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id      UUID         NOT NULL REFERENCES message(message_id) ON DELETE CASCADE,
    file_type       VARCHAR(20)  NOT NULL,
    file_url        TEXT         NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT,
    mime_type       VARCHAR(100)
);

-- =========================================================
-- 10. read_receipt
-- =========================================================
CREATE TABLE IF NOT EXISTS read_receipt (
    receipt_id   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id   UUID        NOT NULL REFERENCES message(message_id) ON DELETE CASCADE,
    recipient_id UUID        NOT NULL,
    read_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =========================================================
-- 11. attendance_report
-- =========================================================
CREATE TABLE IF NOT EXISTS attendance_report (
    report_id       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id        UUID        NOT NULL REFERENCES classroom(class_id) ON DELETE RESTRICT,
    month           INTEGER     NOT NULL CHECK (month BETWEEN 1 AND 12),
    year            INTEGER     NOT NULL,
    generated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    generated_by    VARCHAR(20) NOT NULL,
    report_file_url TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'GENERATED',
    summary         JSONB
);

-- =========================================================
-- Indexes
-- =========================================================
CREATE INDEX IF NOT EXISTS idx_tsm_teacher     ON teacher_subject_mapping(teacher_id);
CREATE INDEX IF NOT EXISTS idx_tsm_class       ON teacher_subject_mapping(class_id);
CREATE INDEX IF NOT EXISTS idx_att_class_date  ON attendance(class_id, date);
CREATE INDEX IF NOT EXISTS idx_att_student     ON attendance(student_id);
CREATE INDEX IF NOT EXISTS idx_msg_sender      ON message(sender_id);
CREATE INDEX IF NOT EXISTS idx_msg_class       ON message(target_class_id);
CREATE INDEX IF NOT EXISTS idx_msg_homework    ON message(target_class_id, homework_due_date) WHERE is_homework = TRUE;
CREATE INDEX IF NOT EXISTS idx_rr_message      ON read_receipt(message_id);
CREATE INDEX IF NOT EXISTS idx_ar_class_period ON attendance_report(class_id, year DESC, month DESC);
CREATE INDEX IF NOT EXISTS idx_student_class   ON tp_student(class_id);
