-- =====================================================================
-- Migration: extend quiz_question with chapter_id / topic_id / difficulty / language
--
-- Why this exists:
--   The original `quiz_question` table predated the learning-system rollout.
--   The new QuizQuestion entity adds four columns. Hibernate's `ddl-auto=update`
--   sometimes tries to create indexes BEFORE adding the columns, which throws
--   `ERROR: column "language" does not exist`. Run this script once to bring
--   the existing table up to date.
--
-- Idempotent: safe to run multiple times.
-- =====================================================================

ALTER TABLE quiz_question
    ADD COLUMN IF NOT EXISTS language    VARCHAR(5)  NOT NULL DEFAULT 'en',
    ADD COLUMN IF NOT EXISTS difficulty  VARCHAR(10),
    ADD COLUMN IF NOT EXISTS chapter_id  BIGINT,
    ADD COLUMN IF NOT EXISTS topic_id    BIGINT;

-- Backfill difficulty for older rows (the enum check expects EASY/MEDIUM/HARD).
UPDATE quiz_question
   SET difficulty = 'MEDIUM'
 WHERE difficulty IS NULL;

-- Link existing rows to chapter ids via the legacy `chapter` text column where possible.
UPDATE quiz_question q
   SET chapter_id = c.id
  FROM chapter c
 WHERE q.chapter_id IS NULL
   AND q.chapter   = c.name;

-- Optional: enforce status enum at the DB level (Hibernate already enforces it
-- in Java, so this is purely defence-in-depth).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_quiz_question_difficulty'
    ) THEN
        ALTER TABLE quiz_question
            ADD CONSTRAINT chk_quiz_question_difficulty
            CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD'));
    END IF;
END$$;

-- Indexes (Hibernate will create these on next boot too; included here so the
-- table is fully ready even if you never restart with ddl-auto=update).
CREATE INDEX IF NOT EXISTS idx_quiz_chapter_diff ON quiz_question (chapter_id, difficulty);
CREATE INDEX IF NOT EXISTS idx_quiz_topic        ON quiz_question (topic_id);
CREATE INDEX IF NOT EXISTS idx_quiz_lang         ON quiz_question (language);

-- Verify
SELECT column_name, data_type, is_nullable, column_default
  FROM information_schema.columns
 WHERE table_name = 'quiz_question'
 ORDER BY ordinal_position;
