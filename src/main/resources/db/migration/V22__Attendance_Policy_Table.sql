CREATE TABLE IF NOT EXISTS attendance_policy (
    id                          BIGSERIAL    PRIMARY KEY,
    school_name                 VARCHAR(200) NOT NULL UNIQUE,
    min_attendance_pct          INT          NOT NULL DEFAULT 75,
    at_risk_drop_pct            INT          NOT NULL DEFAULT 10,
    auto_notify_parents         BOOLEAN      NOT NULL DEFAULT FALSE,
    auto_notify_threshold_pct   INT          NOT NULL DEFAULT 75,
    auto_notify_cooldown_days   INT          NOT NULL DEFAULT 7,
    last_updated_by             VARCHAR(255),
    last_updated_at             TIMESTAMPTZ
);
