CREATE TABLE IF NOT EXISTS attendance_audit (
    id                    BIGSERIAL    PRIMARY KEY,
    attendance_id         UUID         NOT NULL REFERENCES attendance(attendance_id) ON DELETE RESTRICT,
    changed_by_user_id    BIGINT       NOT NULL,
    changed_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    old_status            VARCHAR(20)  NOT NULL,
    new_status            VARCHAR(20)  NOT NULL,
    old_reason_code       VARCHAR(20),
    new_reason_code       VARCHAR(20),
    note                  TEXT
);

CREATE INDEX IF NOT EXISTS idx_att_audit_attendance_id ON attendance_audit(attendance_id);
CREATE INDEX IF NOT EXISTS idx_att_audit_changed_at    ON attendance_audit(changed_at);
