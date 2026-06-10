-- Link teacher portal Teacher entity to user_login (Long PK)
ALTER TABLE teacher ADD COLUMN IF NOT EXISTS user_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_teacher_user_id ON teacher(user_id);
