CREATE TABLE parent_teacher_message (
    id             BIGSERIAL    PRIMARY KEY,
    parent_user_id BIGINT       NOT NULL,
    class_name     VARCHAR(50)  NOT NULL,
    sender_role    VARCHAR(20)  NOT NULL DEFAULT 'parent',
    sender_name    VARCHAR(255),
    body           TEXT         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ptm_parent_user ON parent_teacher_message(parent_user_id);
CREATE INDEX idx_ptm_class_name  ON parent_teacher_message(class_name);
