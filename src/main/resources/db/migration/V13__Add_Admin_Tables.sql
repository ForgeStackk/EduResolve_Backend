-- ── Enhance complaint table (used as tickets) ────────────────────────────────
ALTER TABLE complaint ADD COLUMN IF NOT EXISTS priority       VARCHAR(20)  DEFAULT 'medium';
ALTER TABLE complaint ADD COLUMN IF NOT EXISTS raised_by_name VARCHAR(255);
ALTER TABLE complaint ADD COLUMN IF NOT EXISTS raised_by_role VARCHAR(20)  DEFAULT 'parent';
ALTER TABLE complaint ADD COLUMN IF NOT EXISTS sla_due_at     TIMESTAMP;
ALTER TABLE complaint ADD COLUMN IF NOT EXISTS resolved_at    TIMESTAMP;
ALTER TABLE complaint ADD COLUMN IF NOT EXISTS assignee_name  VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_complaint_status     ON complaint(status);
CREATE INDEX IF NOT EXISTS idx_complaint_created_at ON complaint(created_at DESC);

-- ── Complaint replies (ticket thread messages) ────────────────────────────────
CREATE TABLE IF NOT EXISTS complaint_reply (
    id           BIGSERIAL PRIMARY KEY,
    complaint_id BIGINT       NOT NULL REFERENCES complaint(id) ON DELETE CASCADE,
    author_id    BIGINT,
    author_name  VARCHAR(255),
    author_role  VARCHAR(20),
    body         TEXT         NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_complaint_reply_complaint_id ON complaint_reply(complaint_id);

-- ── Enhance fee table ─────────────────────────────────────────────────────────
ALTER TABLE fee ADD COLUMN IF NOT EXISTS paid_amount NUMERIC(10,2) DEFAULT 0;
ALTER TABLE fee ADD COLUMN IF NOT EXISTS paid_at     TIMESTAMP;
ALTER TABLE fee ADD COLUMN IF NOT EXISTS grade       VARCHAR(20);
ALTER TABLE fee ADD COLUMN IF NOT EXISTS section     VARCHAR(5);

CREATE INDEX IF NOT EXISTS idx_fee_status   ON fee(status);
CREATE INDEX IF NOT EXISTS idx_fee_due_date ON fee(due_date);

-- ── Broadcast table ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS broadcast (
    id              BIGSERIAL    PRIMARY KEY,
    sent_by_id      BIGINT,
    sent_by_name    VARCHAR(255),
    channels        VARCHAR(255) NOT NULL,
    audience_grades VARCHAR(255),
    message         TEXT         NOT NULL,
    is_emergency    BOOLEAN      NOT NULL DEFAULT FALSE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending',
    recipient_count INT          NOT NULL DEFAULT 0,
    scheduled_at    TIMESTAMP,
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_broadcast_created_at ON broadcast(created_at DESC);

-- ── Audit log table ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGSERIAL    PRIMARY KEY,
    actor_id    BIGINT,
    actor_name  VARCHAR(255),
    actor_role  VARCHAR(20),
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id   VARCHAR(100),
    ip          VARCHAR(50),
    detail      TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_actor_id   ON audit_log(actor_id);
