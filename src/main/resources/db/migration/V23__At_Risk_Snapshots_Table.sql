CREATE TABLE IF NOT EXISTS at_risk_snapshots (
    id               BIGSERIAL   PRIMARY KEY,
    student_id       UUID        NOT NULL REFERENCES tp_student(student_id) ON DELETE RESTRICT,
    snapshot_date    DATE        NOT NULL,
    attendance_pct   DECIMAL(5,2),
    previous_pct     DECIMAL(5,2),
    reason_dominant  VARCHAR(20),
    notified_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_at_risk_student_date UNIQUE (student_id, snapshot_date)
);

CREATE INDEX IF NOT EXISTS idx_at_risk_student_id ON at_risk_snapshots(student_id);
