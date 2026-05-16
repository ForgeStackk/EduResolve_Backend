-- Migration script to add new columns to user_login table
-- This script adds firstName, lastName, username, and schoolName columns
-- and migrates existing data from the name column if it exists

-- Add new columns
ALTER TABLE user_login 
ADD COLUMN IF NOT EXISTS first_name VARCHAR(50),
ADD COLUMN IF NOT EXISTS last_name VARCHAR(50),
ADD COLUMN IF NOT EXISTS username VARCHAR(50) UNIQUE,
ADD COLUMN IF NOT EXISTS school_name VARCHAR(200);

-- Migrate existing data from name column to first_name and last_name
-- This assumes the existing name column contains full name
-- We'll split it into first and last name (simple split on first space)
UPDATE user_login 
SET first_name = SUBSTRING(name FROM 1 FOR POSITION(' ' IN name) - 1),
    last_name = SUBSTRING(name FROM POSITION(' ' IN name) + 1)
WHERE name IS NOT NULL 
  AND POSITION(' ' IN name) > 0
  AND first_name IS NULL;

-- For names without spaces, put everything in first_name
UPDATE user_login 
SET first_name = name
WHERE name IS NOT NULL 
  AND (POSITION(' ' IN name) = 0 OR POSITION(' ' IN name) IS NULL)
  AND first_name IS NULL;

-- Set NOT NULL constraint on first_name after migration
ALTER TABLE user_login 
ALTER COLUMN first_name SET NOT NULL;

ALTER TABLE user_login 
ALTER COLUMN last_name SET NOT NULL;

ALTER TABLE user_login 
ALTER COLUMN username SET NOT NULL;

-- Drop the old name column (optional - comment out if you want to keep it for backup)
-- ALTER TABLE user_login DROP COLUMN IF EXISTS name;

-- Add comments to document the columns
COMMENT ON COLUMN user_login.first_name IS 'First name of the user';
COMMENT ON COLUMN user_login.last_name IS 'Last name of the user';
COMMENT ON COLUMN user_login.school_name IS 'Name of the school the user attends';
