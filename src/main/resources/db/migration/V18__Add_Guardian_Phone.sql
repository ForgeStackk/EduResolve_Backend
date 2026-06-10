ALTER TABLE user_login
    ADD COLUMN IF NOT EXISTS guardian_phone VARCHAR(20);
