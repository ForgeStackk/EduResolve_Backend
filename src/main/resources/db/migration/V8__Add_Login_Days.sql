-- Track cumulative login days for plant-growth stages (separate from consecutive streak).
-- login_days  : incremented once per calendar day (IST) on each student login.
-- last_login_date : the most recent day a login was recorded; prevents double-counting.

ALTER TABLE student_profile
    ADD COLUMN IF NOT EXISTS login_days       INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_login_date  DATE;
