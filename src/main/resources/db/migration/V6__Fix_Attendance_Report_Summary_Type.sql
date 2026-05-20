ALTER TABLE attendance_report
    ALTER COLUMN summary TYPE TEXT USING summary::TEXT;
