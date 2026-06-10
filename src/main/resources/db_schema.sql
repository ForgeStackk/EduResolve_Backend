-- EduResolve User Login Table
-- Run this script in PostgreSQL to create the user_login table

CREATE TABLE IF NOT EXISTS user_login (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  class_name VARCHAR(10),
  email VARCHAR(100) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(20),
  phone_number VARCHAR(20),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on email for faster lookups
CREATE INDEX IF NOT EXISTS idx_user_login_email ON user_login(email);

-- Create index on role for filtering by role
CREATE INDEX IF NOT EXISTS idx_user_login_role ON user_login(role);

-- Insert a test user (password: password123 hashed with BCrypt)
-- Note: In production, never hardcode passwords. Use the registration API instead.
-- INSERT INTO user_login (name, class_name, email, password, role, phone_number) 
-- VALUES ('Test User', '10A', 'test@example.com', '$2a$10$slYQmyNdGzin7olVN3p5aOSvzRKgw5X8KwSvnnQvtQWLMh8GyISqm', 'student', '1234567890');
