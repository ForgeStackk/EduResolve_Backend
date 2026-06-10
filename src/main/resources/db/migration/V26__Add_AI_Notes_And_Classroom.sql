-- V26__Add_AI_Notes_And_Classroom.sql
-- AI Notes Generator + Student Classroom features
-- All PKs: BIGSERIAL. All FKs: BIGINT. Zero UUID.

-- =====================================================================
-- 1. student_note_preferences  (one row per student)
-- =====================================================================
CREATE TABLE IF NOT EXISTS student_note_preferences (
    id                     BIGSERIAL    PRIMARY KEY,
    student_id             BIGINT       NOT NULL UNIQUE REFERENCES student_profile(id),
    preferred_language     VARCHAR(10)  NOT NULL DEFAULT 'en',
    preferred_note_length  VARCHAR(20)           DEFAULT 'STANDARD',
    updated_at             TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- =====================================================================
-- 2. pdf_extraction_jobs
-- =====================================================================
CREATE TABLE IF NOT EXISTS pdf_extraction_jobs (
    id               BIGSERIAL    PRIMARY KEY,
    student_id       BIGINT       NOT NULL REFERENCES student_profile(id),
    file_url         VARCHAR(500) NOT NULL,
    file_name        VARCHAR(255) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING',
    extracted_text   TEXT,
    page_count       INT,
    character_count  INT,
    failure_reason   TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    completed_at     TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_pdf_jobs_student ON pdf_extraction_jobs(student_id, status, created_at DESC);

-- =====================================================================
-- 3. student_classrooms
--    New feature table — NOT the teacher-portal classroom (which uses UUID PK).
--    classroom_seq_id loosely references classroom.seq_id (Option A: no DB-level FK
--    because classroom.seq_id is not a PK; uniqueness only).
-- =====================================================================
CREATE TABLE IF NOT EXISTS student_classrooms (
    id               BIGSERIAL    PRIMARY KEY,
    classroom_seq_id BIGINT,
    school_name      VARCHAR(200) NOT NULL,
    class_label      VARCHAR(50)  NOT NULL,
    name             VARCHAR(255) NOT NULL,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_student_classroom UNIQUE (class_label, school_name)
);
CREATE INDEX IF NOT EXISTS idx_student_classrooms_label ON student_classrooms(class_label, school_name);

-- =====================================================================
-- 4. student_notes
-- =====================================================================
CREATE TABLE IF NOT EXISTS student_notes (
    id                      BIGSERIAL    PRIMARY KEY,
    student_id              BIGINT       NOT NULL REFERENCES student_profile(id),
    school_name             VARCHAR(200) NOT NULL,
    title                   VARCHAR(255) NOT NULL,
    content                 TEXT         NOT NULL,
    raw_prompt              TEXT,
    source_type             VARCHAR(30)  NOT NULL,
    language                VARCHAR(10)  NOT NULL DEFAULT 'en',
    subject_id              BIGINT                REFERENCES subject(id),
    chapter_ref             VARCHAR(255),
    source_file_url         VARCHAR(500),
    source_file_name        VARCHAR(255),
    source_page_count       INT,
    ai_model_used           VARCHAR(100),
    is_edited               BOOLEAN      NOT NULL DEFAULT FALSE,
    is_pinned               BOOLEAN      NOT NULL DEFAULT FALSE,
    is_shared_to_classroom  BOOLEAN      NOT NULL DEFAULT FALSE,
    shared_classroom_id     BIGINT                REFERENCES student_classrooms(id),
    tags                    TEXT,
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at              TIMESTAMP,
    restored_at             TIMESTAMP,
    is_archived             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_notes_student_active  ON student_notes(student_id, is_active, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notes_student_subject ON student_notes(student_id, subject_id, is_active);
CREATE INDEX IF NOT EXISTS idx_notes_student_lang    ON student_notes(student_id, language, is_active);
CREATE INDEX IF NOT EXISTS idx_notes_student_trash   ON student_notes(student_id, is_active, deleted_at);

-- =====================================================================
-- 5. note_versions  (max 10 per note — enforced at service layer)
-- =====================================================================
CREATE TABLE IF NOT EXISTS note_versions (
    id               BIGSERIAL    PRIMARY KEY,
    note_id          BIGINT       NOT NULL REFERENCES student_notes(id),
    version_number   INT          NOT NULL,
    content_snapshot TEXT         NOT NULL,
    language         VARCHAR(10)  NOT NULL,
    edited_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (note_id, version_number)
);
CREATE INDEX IF NOT EXISTS idx_note_versions_note ON note_versions(note_id, version_number DESC);

-- =====================================================================
-- 6. note_revision_log
-- =====================================================================
CREATE TABLE IF NOT EXISTS note_revision_log (
    id          BIGSERIAL   PRIMARY KEY,
    note_id     BIGINT      NOT NULL REFERENCES student_notes(id),
    student_id  BIGINT      NOT NULL REFERENCES student_profile(id),
    revised_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    result      VARCHAR(20) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_revision_log_note ON note_revision_log(note_id, revised_at DESC);

-- =====================================================================
-- 7. classroom_subject_rooms
-- =====================================================================
CREATE TABLE IF NOT EXISTS classroom_subject_rooms (
    id                  BIGSERIAL    PRIMARY KEY,
    classroom_id        BIGINT       NOT NULL REFERENCES student_classrooms(id),
    subject_id          BIGINT       NOT NULL REFERENCES subject(id),
    name                VARCHAR(255) NOT NULL,
    created_by_user_id  BIGINT       NOT NULL REFERENCES user_login(id),
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_subjectroom_classroom ON classroom_subject_rooms(classroom_id, is_active);

-- =====================================================================
-- 8. classroom_messages
-- =====================================================================
CREATE TABLE IF NOT EXISTS classroom_messages (
    id                   BIGSERIAL    PRIMARY KEY,
    room_id              BIGINT       NOT NULL,
    room_type            VARCHAR(20)  NOT NULL,
    sender_id            BIGINT       NOT NULL REFERENCES user_login(id),
    sender_role          VARCHAR(20)  NOT NULL,
    message_type         VARCHAR(20)  NOT NULL,
    text_content         TEXT,
    attachment_url       VARCHAR(500),
    attachment_type      VARCHAR(100),
    attachment_name      VARCHAR(255),
    shared_note_id       BIGINT                REFERENCES student_notes(id),
    reply_to_message_id  BIGINT                REFERENCES classroom_messages(id),
    is_pinned            BOOLEAN      NOT NULL DEFAULT FALSE,
    pinned_by_user_id    BIGINT                REFERENCES user_login(id),
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_by_user_id   BIGINT                REFERENCES user_login(id),
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_classroom_msg_room   ON classroom_messages(room_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_classroom_msg_sender ON classroom_messages(sender_id, created_at DESC);

-- =====================================================================
-- 9. classroom_message_reactions
-- =====================================================================
CREATE TABLE IF NOT EXISTS classroom_message_reactions (
    id          BIGSERIAL   PRIMARY KEY,
    message_id  BIGINT      NOT NULL REFERENCES classroom_messages(id),
    user_id     BIGINT      NOT NULL REFERENCES user_login(id),
    emoji       VARCHAR(10) NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (message_id, user_id, emoji)
);
CREATE INDEX IF NOT EXISTS idx_reactions_message ON classroom_message_reactions(message_id);

-- =====================================================================
-- 10. classroom_members
-- =====================================================================
CREATE TABLE IF NOT EXISTS classroom_members (
    id            BIGSERIAL   PRIMARY KEY,
    classroom_id  BIGINT      NOT NULL REFERENCES student_classrooms(id),
    user_id       BIGINT      NOT NULL REFERENCES user_login(id),
    role          VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    joined_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    last_seen_at  TIMESTAMP,
    is_online     BOOLEAN     NOT NULL DEFAULT FALSE,
    UNIQUE (classroom_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_classroom_members ON classroom_members(classroom_id, user_id);

-- =====================================================================
-- 11. classroom_reported_messages
-- =====================================================================
CREATE TABLE IF NOT EXISTS classroom_reported_messages (
    id                    BIGSERIAL  PRIMARY KEY,
    message_id            BIGINT     NOT NULL REFERENCES classroom_messages(id),
    reported_by_user_id   BIGINT     NOT NULL REFERENCES user_login(id),
    reason                TEXT       NOT NULL,
    reported_at           TIMESTAMP  NOT NULL DEFAULT NOW(),
    reviewed_by_user_id   BIGINT               REFERENCES user_login(id),
    reviewed_at           TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_reported_messages ON classroom_reported_messages(message_id);

-- =====================================================================
-- 12. classroom_pinned_messages  (max 5 per room — enforced at service layer)
-- =====================================================================
CREATE TABLE IF NOT EXISTS classroom_pinned_messages (
    id                 BIGSERIAL  PRIMARY KEY,
    room_id            BIGINT     NOT NULL,
    message_id         BIGINT     NOT NULL REFERENCES classroom_messages(id),
    pinned_by_user_id  BIGINT     NOT NULL REFERENCES user_login(id),
    pinned_at          TIMESTAMP  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_pinned_messages_room ON classroom_pinned_messages(room_id, pinned_at DESC);
