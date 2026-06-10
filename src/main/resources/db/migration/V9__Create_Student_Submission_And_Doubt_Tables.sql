-- ── Homework submissions (student → teacher) ────────────────────────────────
-- All IDs are BIGINT sequences. Person references use user_login.id (BIGINT).
-- assignment_id references message.msg_num which is added in V10.

CREATE SEQUENCE hw_submission_seq    START 1 INCREMENT 1;
CREATE SEQUENCE hw_submission_att_seq START 1 INCREMENT 1;
CREATE SEQUENCE doubt_thread_seq      START 1 INCREMENT 1;
CREATE SEQUENCE doubt_message_seq     START 1 INCREMENT 1;
CREATE SEQUENCE doubt_message_att_seq START 1 INCREMENT 1;

CREATE TABLE homework_submission (
    submission_id   BIGINT      PRIMARY KEY DEFAULT nextval('hw_submission_seq'),
    assignment_id   BIGINT      NOT NULL,
    student_id      BIGINT      NOT NULL REFERENCES user_login(id) ON DELETE CASCADE,
    text_caption    TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    submitted_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at     TIMESTAMPTZ,
    graded_at       TIMESTAMPTZ
);

CREATE INDEX idx_hs_assignment ON homework_submission(assignment_id);
CREATE INDEX idx_hs_student    ON homework_submission(student_id);

CREATE TABLE homework_submission_attachment (
    attachment_id   BIGINT      PRIMARY KEY DEFAULT nextval('hw_submission_att_seq'),
    submission_id   BIGINT      NOT NULL REFERENCES homework_submission(submission_id) ON DELETE CASCADE,
    file_type       VARCHAR(20) NOT NULL,
    file_url        TEXT        NOT NULL,
    file_name       VARCHAR(500),
    file_size_bytes BIGINT,
    mime_type       VARCHAR(100)
);

CREATE INDEX idx_hsa_submission ON homework_submission_attachment(submission_id);

-- ── Student → teacher doubt threads ─────────────────────────────────────────

CREATE TABLE doubt_thread (
    thread_id       BIGINT      PRIMARY KEY DEFAULT nextval('doubt_thread_seq'),
    student_id      BIGINT      NOT NULL REFERENCES user_login(id) ON DELETE CASCADE,
    teacher_id      BIGINT      NOT NULL REFERENCES user_login(id) ON DELETE CASCADE,
    subject_id      BIGINT,
    chapter_id      BIGINT,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at     TIMESTAMPTZ
);

CREATE INDEX idx_dt_student ON doubt_thread(student_id);
CREATE INDEX idx_dt_teacher ON doubt_thread(teacher_id);
CREATE INDEX idx_dt_status  ON doubt_thread(status);

CREATE TABLE doubt_message (
    doubt_message_id BIGINT      PRIMARY KEY DEFAULT nextval('doubt_message_seq'),
    thread_id        BIGINT      NOT NULL REFERENCES doubt_thread(thread_id) ON DELETE CASCADE,
    sender_id        BIGINT      NOT NULL REFERENCES user_login(id) ON DELETE CASCADE,
    sender_role      VARCHAR(20) NOT NULL,
    text_body        TEXT,
    sent_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_dm_thread ON doubt_message(thread_id);

CREATE TABLE doubt_message_attachment (
    attachment_id    BIGINT      PRIMARY KEY DEFAULT nextval('doubt_message_att_seq'),
    doubt_message_id BIGINT      NOT NULL REFERENCES doubt_message(doubt_message_id) ON DELETE CASCADE,
    file_type        VARCHAR(20) NOT NULL,
    file_url         TEXT        NOT NULL,
    file_name        VARCHAR(500),
    file_size_bytes  BIGINT,
    mime_type        VARCHAR(100)
);

CREATE INDEX idx_dma_msg ON doubt_message_attachment(doubt_message_id);
