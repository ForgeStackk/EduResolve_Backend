-- =====================================================================
-- EduResolve - PostgreSQL schema
-- Generated to match the JPA entities under
--   com.forgeStackk.EduResolve.entity.*
--
-- Usage:
--   psql -U postgres -d eduresolve -f schema_postgres.sql
--
-- Notes:
--   * Hibernate (ddl-auto=update) will create these tables automatically
--     when the app boots. This script is provided for reference and for
--     environments where you want to manage the schema manually.
--   * All scripts are idempotent (CREATE ... IF NOT EXISTS).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. user_login
--    Entity: UserLogin
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_login (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    class_name    VARCHAR(10),
    email         VARCHAR(100) UNIQUE NOT NULL,
    password      VARCHAR(255) NOT NULL,
    role          VARCHAR(20),
    phone_number  VARCHAR(20)
);

CREATE INDEX IF NOT EXISTS idx_user_login_email ON user_login(email);
CREATE INDEX IF NOT EXISTS idx_user_login_role  ON user_login(role);


-- ---------------------------------------------------------------------
-- 2. homework
--    Entity: Homework
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS homework (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    due_date        DATE,
    has_attachment  BOOLEAN NOT NULL DEFAULT FALSE,
    teacher_id      BIGINT,
    class_name      VARCHAR(50),
    subject         VARCHAR(100),
    created_at      TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_homework_teacher_id  ON homework(teacher_id);
CREATE INDEX IF NOT EXISTS idx_homework_class_name  ON homework(class_name);
CREATE INDEX IF NOT EXISTS idx_homework_created_at  ON homework(created_at DESC);


-- ---------------------------------------------------------------------
-- 3. doubt
--    Entity: Doubt
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS doubt (
    id          BIGSERIAL PRIMARY KEY,
    student_id  BIGINT,
    query       TEXT NOT NULL,
    answer      TEXT,
    is_helpful  BOOLEAN,
    subject     VARCHAR(100),
    created_at  TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_doubt_student_id ON doubt(student_id);
CREATE INDEX IF NOT EXISTS idx_doubt_created_at ON doubt(created_at DESC);


-- ---------------------------------------------------------------------
-- 4. complaint
--    Entity: Complaint
--    Status enum stored as VARCHAR (Pending | InReview | Resolved)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS complaint (
    id           BIGSERIAL PRIMARY KEY,
    parent_id    BIGINT,
    category     VARCHAR(100),
    subject      VARCHAR(255) NOT NULL,
    description  TEXT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'Pending'
                 CHECK (status IN ('Pending', 'InReview', 'Resolved')),
    created_at   TIMESTAMP WITHOUT TIME ZONE,
    updated_at   TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_complaint_parent_id  ON complaint(parent_id);
CREATE INDEX IF NOT EXISTS idx_complaint_status     ON complaint(status);
CREATE INDEX IF NOT EXISTS idx_complaint_created_at ON complaint(created_at DESC);


-- ---------------------------------------------------------------------
-- 5. fee
--    Entity: Fee
--    Status enum stored as VARCHAR (Paid | Unpaid)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fee (
    id                BIGSERIAL PRIMARY KEY,
    student_id        BIGINT,
    student_name      VARCHAR(255),
    class_name        VARCHAR(50),
    phone             VARCHAR(20),
    amount            NUMERIC(10, 2) NOT NULL DEFAULT 0,
    due_date          DATE,
    status            VARCHAR(10) NOT NULL DEFAULT 'Unpaid'
                      CHECK (status IN ('Paid', 'Unpaid')),
    last_reminder_at  TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_fee_student_id ON fee(student_id);
CREATE INDEX IF NOT EXISTS idx_fee_status     ON fee(status);


-- ---------------------------------------------------------------------
-- 6. notice
--    Entity: Notice
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notice (
    id               BIGSERIAL PRIMARY KEY,
    target_audience  VARCHAR(100),
    message          TEXT NOT NULL,
    channels         VARCHAR(100),     -- comma-separated, e.g. 'whatsapp,sms'
    sent_at          TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_notice_sent_at ON notice(sent_at DESC);


-- ---------------------------------------------------------------------
-- 7. school_event
--    Entity: SchoolEvent
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS school_event (
    id                BIGSERIAL PRIMARY KEY,
    title             VARCHAR(255) NOT NULL,
    location          VARCHAR(255),
    event_date        DATE NOT NULL,
    event_time        VARCHAR(20),
    attendees_count   INTEGER
);

CREATE INDEX IF NOT EXISTS idx_school_event_date ON school_event(event_date);


-- ---------------------------------------------------------------------
-- 8. quiz_question
--    Entity: QuizQuestion
--    options_json stores JSON like:
--      [{"id":"o1","text":"..."},{"id":"o2","text":"..."}]
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS quiz_question (
    id                 BIGSERIAL PRIMARY KEY,
    subject            VARCHAR(100),
    chapter            VARCHAR(255),
    text               TEXT NOT NULL,
    options_json       TEXT,
    correct_option_id  VARCHAR(50),
    explanation        TEXT
);

CREATE INDEX IF NOT EXISTS idx_quiz_question_subject_chapter
    ON quiz_question(subject, chapter);


-- ---------------------------------------------------------------------
-- 9. student_profile
--    Entity: StudentProfile
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS student_profile (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT,
    name                VARCHAR(255) NOT NULL,
    initials            VARCHAR(10),
    color               VARCHAR(20),
    engagement          INTEGER,
    grade               VARCHAR(10),
    status              VARCHAR(30),     -- excellent | good | at-risk
    class_name          VARCHAR(50),
    streak_days         INTEGER,
    experience_points   INTEGER,
    top_percentage      INTEGER
);

CREATE INDEX IF NOT EXISTS idx_student_profile_user_id    ON student_profile(user_id);
CREATE INDEX IF NOT EXISTS idx_student_profile_class_name ON student_profile(class_name);


-- =====================================================================
-- Optional foreign-key constraints (uncomment if you want strict FK
-- enforcement at the DB level. Hibernate does NOT add these because the
-- entities use plain Long IDs rather than @ManyToOne associations).
-- =====================================================================
 ALTER TABLE homework         ADD CONSTRAINT fk_homework_teacher  FOREIGN KEY (teacher_id) REFERENCES user_login(id) ON DELETE SET NULL;
 ALTER TABLE doubt            ADD CONSTRAINT fk_doubt_student    FOREIGN KEY (student_id) REFERENCES user_login(id) ON DELETE SET NULL;
 ALTER TABLE complaint        ADD CONSTRAINT fk_complaint_parent FOREIGN KEY (parent_id)  REFERENCES user_login(id) ON DELETE SET NULL;
 ALTER TABLE fee              ADD CONSTRAINT fk_fee_student      FOREIGN KEY (student_id) REFERENCES user_login(id) ON DELETE CASCADE;
 ALTER TABLE student_profile  ADD CONSTRAINT fk_profile_user     FOREIGN KEY (user_id)    REFERENCES user_login(id) ON DELETE CASCADE;


-- =====================================================================
-- Quick sanity check
-- =====================================================================
-- \dt
SELECT table_name FROM information_schema.tables
   WHERE table_schema = 'public' ORDER BY table_name;
