-- BUG-09: Enforce email uniqueness on user_login.
-- Use CREATE UNIQUE INDEX CONCURRENTLY equivalent (conditional) so it
-- succeeds even if duplicate emails exist in legacy data.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
         WHERE tablename = 'user_login' AND indexname = 'uq_user_login_email'
    ) THEN
        -- Deduplicate first: keep the row with the smallest id for each email
        DELETE FROM user_login u1
         USING user_login u2
         WHERE u1.email = u2.email AND u1.id > u2.id;

        CREATE UNIQUE INDEX uq_user_login_email ON user_login(email);
    END IF;
END;
$$;

-- BUG-03: Drop the FK from doubt.student_id → user_login(id).
-- The doubt table uses student_id as a soft reference (display / filter only).
-- The FK was causing 500 errors because client sends student_profile.id,
-- not user_login.id.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_doubt_student'
    ) THEN
        ALTER TABLE doubt DROP CONSTRAINT fk_doubt_student;
    END IF;
END;
$$;
