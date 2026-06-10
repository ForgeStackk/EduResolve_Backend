CREATE TABLE IF NOT EXISTS scheduled_job_log (
    id                  BIGSERIAL    PRIMARY KEY,
    job_name            VARCHAR(100) NOT NULL,
    ran_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    students_scanned    INT          NOT NULL DEFAULT 0,
    at_risk_found       INT          NOT NULL DEFAULT 0,
    notifications_sent  INT          NOT NULL DEFAULT 0,
    errors              TEXT
);

CREATE TABLE IF NOT EXISTS attendance_anomalies (
    id                        BIGSERIAL   PRIMARY KEY,
    class_id                  VARCHAR(20) NOT NULL,
    detected_date             DATE        NOT NULL,
    class_avg_pct             DECIMAL(5,2) NOT NULL,
    expected_pct              DECIMAL(5,2) NOT NULL,
    drop_amount               DECIMAL(5,2) NOT NULL,
    acknowledged_by_user_id   BIGINT,
    acknowledged_at           TIMESTAMPTZ,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_anomalies_class_date ON attendance_anomalies(class_id, detected_date);
CREATE INDEX IF NOT EXISTS idx_anomalies_unacked     ON attendance_anomalies(acknowledged_at) WHERE acknowledged_at IS NULL;
