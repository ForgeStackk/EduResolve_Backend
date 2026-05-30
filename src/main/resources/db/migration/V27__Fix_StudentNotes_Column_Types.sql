-- V27__Fix_StudentNotes_Column_Types.sql
-- The student_notes table may have been created by Hibernate ddl-auto:update
-- before V26 ran (V26 uses CREATE TABLE IF NOT EXISTS, which was a no-op on
-- those databases). On those schemas, content/title were created as bytea,
-- which causes PostgreSQL to reject lower() at query-prepare time even when
-- the :search parameter is null.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'student_notes'
          AND column_name = 'content'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE student_notes
            ALTER COLUMN content TYPE TEXT USING convert_from(content, 'UTF8');
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'student_notes'
          AND column_name = 'title'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE student_notes
            ALTER COLUMN title TYPE VARCHAR(255) USING convert_from(title, 'UTF8');
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'student_notes'
          AND column_name = 'raw_prompt'
          AND data_type = 'bytea'
    ) THEN
        ALTER TABLE student_notes
            ALTER COLUMN raw_prompt TYPE TEXT USING convert_from(raw_prompt, 'UTF8');
    END IF;
END $$;
