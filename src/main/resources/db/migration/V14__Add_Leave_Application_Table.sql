CREATE TABLE IF NOT EXISTS leave_application (
    id            BIGSERIAL PRIMARY KEY,
    student_name  VARCHAR(255) NOT NULL,
    class_name    VARCHAR(50),
    parent_user_id BIGINT,
    from_date     DATE NOT NULL,
    to_date       DATE NOT NULL,
    reason        TEXT NOT NULL,
    status        VARCHAR(30) NOT NULL DEFAULT 'Pending',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at   TIMESTAMPTZ,
    reviewed_by   VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_leave_application_parent_user ON leave_application(parent_user_id);
CREATE INDEX IF NOT EXISTS idx_leave_application_class ON leave_application(class_name);
