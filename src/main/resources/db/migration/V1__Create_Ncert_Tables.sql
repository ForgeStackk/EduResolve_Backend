-- NCERT Learning Platform Database Schema
-- Version 1.0
-- PostgreSQL Database

-- Create the ncert_learning database if it doesn't exist
-- CREATE DATABASE IF NOT EXISTS ncert_learning;

-- Use the ncert_learning database
-- \c ncert_learning;

-- Enable UUID extension for generating unique IDs
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create custom types for enums
DO $$$ BEGIN
    CREATE TYPE block_type AS ENUM ('TEXT', 'IMAGE', 'DIAGRAM', 'TABLE', 'HEADING');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$$$;

DO $$$ BEGIN
    CREATE TYPE difficulty_level AS ENUM ('EASY', 'MEDIUM', 'HARD');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$$$;

-- Create ncert_books table
CREATE TABLE IF NOT EXISTS ncert_books (
    id BIGSERIAL PRIMARY KEY,
    class_grade VARCHAR(50) NOT NULL,
    subject VARCHAR(100) NOT NULL,
    title VARCHAR(500) NOT NULL,
    github_url TEXT,
    github_repo VARCHAR(200),
    github_path TEXT,
    pdf_filename VARCHAR(255),
    total_pages INTEGER DEFAULT 0,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes for faster queries
    CONSTRAINT idx_ncert_books_class_subject UNIQUE (class_grade, subject, title)
);

-- Create indexes for ncert_books
CREATE INDEX IF NOT EXISTS idx_ncert_books_class_grade ON ncert_books(class_grade);
CREATE INDEX IF NOT EXISTS idx_ncert_books_subject ON ncert_books(subject);
CREATE INDEX IF NOT EXISTS idx_ncert_books_title ON ncert_books USING gin(to_tsvector('english', title));

-- Create ncert_chapters table
CREATE TABLE IF NOT EXISTS ncert_chapters (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    title VARCHAR(500) NOT NULL,
    chapter_number INTEGER NOT NULL,
    order_index INTEGER NOT NULL DEFAULT 0,
    summary TEXT,
    start_page INTEGER DEFAULT 1,
    end_page INTEGER DEFAULT 1,
    
    -- Foreign key constraint
    CONSTRAINT fk_chapters_book FOREIGN KEY (book_id) REFERENCES ncert_books(id) ON DELETE CASCADE,
    
    -- Unique constraint for chapter number within a book
    CONSTRAINT idx_chapters_book_chapter UNIQUE (book_id, chapter_number)
);

-- Create indexes for ncert_chapters
CREATE INDEX IF NOT EXISTS idx_ncert_chapters_book_id ON ncert_chapters(book_id);
CREATE INDEX IF NOT EXISTS idx_ncert_chapters_order ON ncert_chapters(book_id, order_index);
CREATE INDEX IF NOT EXISTS idx_ncert_chapters_title ON ncert_chapters USING gin(to_tsvector('english', title));

-- Create content_blocks table
CREATE TABLE IF NOT EXISTS content_blocks (
    id BIGSERIAL PRIMARY KEY,
    chapter_id BIGINT NOT NULL,
    block_type block_type NOT NULL DEFAULT 'TEXT',
    content_text TEXT,
    image_url TEXT,
    image_filename VARCHAR(255),
    order_index INTEGER NOT NULL DEFAULT 0,
    heading VARCHAR(500),
    page_number INTEGER DEFAULT 1,
    
    -- Foreign key constraint
    CONSTRAINT fk_content_blocks_chapter FOREIGN KEY (chapter_id) REFERENCES ncert_chapters(id) ON DELETE CASCADE
);

-- Create indexes for content_blocks
CREATE INDEX IF NOT EXISTS idx_content_blocks_chapter_id ON content_blocks(chapter_id);
CREATE INDEX IF NOT EXISTS idx_content_blocks_order ON content_blocks(chapter_id, order_index);
CREATE INDEX IF NOT EXISTS idx_content_blocks_type ON content_blocks(block_type);
CREATE INDEX IF NOT EXISTS idx_content_blocks_page ON content_blocks(chapter_id, page_number);
CREATE INDEX IF NOT EXISTS idx_content_blocks_text ON content_blocks USING gin(to_tsvector('english', content_text));

-- Create question_bank table
CREATE TABLE IF NOT EXISTS question_bank (
    id BIGSERIAL PRIMARY KEY,
    chapter_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    options TEXT[] NOT NULL, -- PostgreSQL array for multiple choice options
    correct_answer VARCHAR(10) NOT NULL, -- A, B, C, or D
    explanation TEXT,
    difficulty difficulty_level DEFAULT 'MEDIUM',
    topic VARCHAR(200),
    
    -- Foreign key constraint
    CONSTRAINT fk_question_bank_chapter FOREIGN KEY (chapter_id) REFERENCES ncert_chapters(id) ON DELETE CASCADE,
    
    -- Check constraint for correct_answer
    CONSTRAINT chk_correct_answer CHECK (correct_answer IN ('A', 'B', 'C', 'D')),
    
    -- Check constraint for options array
    CONSTRAINT chk_options_length CHECK (array_length(options, 1) = 4)
);

-- Create indexes for question_bank
CREATE INDEX IF NOT EXISTS idx_question_bank_chapter_id ON question_bank(chapter_id);
CREATE INDEX IF NOT EXISTS idx_question_bank_difficulty ON question_bank(difficulty);
CREATE INDEX IF NOT EXISTS idx_question_bank_topic ON question_bank(topic);
CREATE INDEX IF NOT EXISTS idx_question_bank_question ON question_bank USING gin(to_tsvector('english', question));

-- Create user_sessions table for tracking user activity
CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id VARCHAR(255) UNIQUE NOT NULL,
    class_level INTEGER DEFAULT 9,
    subject VARCHAR(100) DEFAULT 'Mathematics',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Check constraint for class level
    CONSTRAINT chk_class_level CHECK (class_level BETWEEN 9 AND 12)
);

-- Create indexes for user_sessions
CREATE INDEX IF NOT EXISTS idx_user_sessions_session_id ON user_sessions(session_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_active ON user_sessions(is_active);
CREATE INDEX IF NOT EXISTS idx_user_sessions_activity ON user_sessions(last_activity);

-- Create ai_conversations table for storing AI Q&A history
CREATE TABLE IF NOT EXISTS ai_conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id VARCHAR(255) NOT NULL,
    class_level INTEGER NOT NULL,
    subject VARCHAR(100) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    context TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key reference to user_sessions (optional)
    CONSTRAINT fk_ai_conversations_session FOREIGN KEY (session_id) REFERENCES user_sessions(session_id) ON DELETE SET NULL
);

-- Create indexes for ai_conversations
CREATE INDEX IF NOT EXISTS idx_ai_conversations_session ON ai_conversations(session_id);
CREATE INDEX IF NOT EXISTS idx_ai_conversations_class ON ai_conversations(class_level);
CREATE INDEX IF NOT EXISTS idx_ai_conversations_subject ON ai_conversations(subject);
CREATE INDEX IF NOT EXISTS idx_ai_conversations_created ON ai_conversations(created_at);
CREATE INDEX IF NOT EXISTS idx_ai_conversations_question ON ai_conversations USING gin(to_tsvector('english', question));

-- Create quiz_results table for storing quiz attempts
CREATE TABLE IF NOT EXISTS quiz_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id VARCHAR(255) NOT NULL,
    book_id BIGINT,
    class_level INTEGER NOT NULL,
    subject VARCHAR(100) NOT NULL,
    difficulty difficulty_level DEFAULT 'MEDIUM',
    total_questions INTEGER NOT NULL,
    correct_answers INTEGER NOT NULL,
    percentage DECIMAL(5,2) NOT NULL,
    time_spent_seconds INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_quiz_results_session FOREIGN KEY (session_id) REFERENCES user_sessions(session_id) ON DELETE SET NULL,
    CONSTRAINT fk_quiz_results_book FOREIGN KEY (book_id) REFERENCES ncert_books(id) ON DELETE SET NULL,
    
    -- Check constraints
    CONSTRAINT chk_percentage CHECK (percentage BETWEEN 0 AND 100),
    CONSTRAINT chk_answers CHECK (correct_answers <= total_questions)
);

-- Create indexes for quiz_results
CREATE INDEX IF NOT EXISTS idx_quiz_results_session ON quiz_results(session_id);
CREATE INDEX IF NOT EXISTS idx_quiz_results_book ON quiz_results(book_id);
CREATE INDEX IF NOT EXISTS idx_quiz_results_class ON quiz_results(class_level);
CREATE INDEX IF NOT EXISTS idx_quiz_results_subject ON quiz_results(subject);
CREATE INDEX IF NOT EXISTS idx_quiz_results_percentage ON quiz_results(percentage);
CREATE INDEX IF NOT EXISTS idx_quiz_results_created ON quiz_results(created_at);

-- Create audit_log table for tracking changes
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    record_id VARCHAR(100) NOT NULL,
    operation VARCHAR(20) NOT NULL, -- INSERT, UPDATE, DELETE
    old_values JSONB,
    new_values JSONB,
    user_id VARCHAR(255),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for audit_log
CREATE INDEX IF NOT EXISTS idx_audit_log_table ON audit_log(table_name);
CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp ON audit_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_log_operation ON audit_log(operation);

-- Create trigger function for updating updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger for ncert_books table
DROP TRIGGER IF EXISTS update_ncert_books_updated_at ON ncert_books;
CREATE TRIGGER update_ncert_books_updated_at
    BEFORE UPDATE ON ncert_books
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create trigger function for audit logging
CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO audit_log (table_name, record_id, operation, old_values, timestamp)
        VALUES (TG_TABLE_NAME, OLD.id::text, TG_OP, row_to_json(OLD), CURRENT_TIMESTAMP);
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audit_log (table_name, record_id, operation, old_values, new_values, timestamp)
        VALUES (TG_TABLE_NAME, NEW.id::text, TG_OP, row_to_json(OLD), row_to_json(NEW), CURRENT_TIMESTAMP);
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO audit_log (table_name, record_id, operation, new_values, timestamp)
        VALUES (TG_TABLE_NAME, NEW.id::text, TG_OP, row_to_json(NEW), CURRENT_TIMESTAMP);
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create audit triggers for main tables
DROP TRIGGER IF EXISTS audit_ncert_books ON ncert_books;
CREATE TRIGGER audit_ncert_books
    AFTER INSERT OR UPDATE OR DELETE ON ncert_books
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

DROP TRIGGER IF EXISTS audit_ncert_chapters ON ncert_chapters;
CREATE TRIGGER audit_ncert_chapters
    AFTER INSERT OR UPDATE OR DELETE ON ncert_chapters
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

DROP TRIGGER IF EXISTS audit_content_blocks ON content_blocks;
CREATE TRIGGER audit_content_blocks
    AFTER INSERT OR UPDATE OR DELETE ON content_blocks
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- Insert sample data for testing
INSERT INTO ncert_books (class_grade, subject, title, github_url, github_repo, github_path, pdf_filename, total_pages) VALUES
('Class 9', 'Mathematics', 'Mathematics - Class 9', 'https://github.com/ncert/class9-maths.pdf', 'ncert/content', 'Class 9/Mathematics/Mathematics-Class-9.pdf', 'Mathematics-Class-9.pdf', 320),
('Class 9', 'Science', 'Science - Class 9', 'https://github.com/ncert/class9-science.pdf', 'ncert/content', 'Class 9/Science/Science-Class-9.pdf', 'Science-Class-9.pdf', 280),
('Class 10', 'Mathematics', 'Mathematics - Class 10', 'https://github.com/ncert/class10-maths.pdf', 'ncert/content', 'Class 10/Mathematics/Mathematics-Class-10.pdf', 'Mathematics-Class-10.pdf', 350),
('Class 10', 'Science', 'Science - Class 10', 'https://github.com/ncert/class10-science.pdf', 'ncert/content', 'Class 10/Science/Science-Class-10.pdf', 'Science-Class-10.pdf', 300)
ON CONFLICT (class_grade, subject, title) DO NOTHING;

-- Insert sample chapters
INSERT INTO ncert_chapters (book_id, title, chapter_number, order_index, summary, start_page, end_page) VALUES
(1, 'Number Systems', 1, 1, 'Introduction to number systems including rational and irrational numbers', 1, 25),
(1, 'Polynomials', 2, 2, 'Understanding polynomials and their operations', 26, 50),
(2, 'Matter in Our Surroundings', 1, 1, 'Basic concepts of matter and its states', 1, 20),
(2, 'Is Matter Around Us Pure', 2, 2, 'Pure substances and mixtures', 21, 40)
ON CONFLICT (book_id, chapter_number) DO NOTHING;

-- Insert sample content blocks
INSERT INTO content_blocks (chapter_id, block_type, content_text, order_index, heading, page_number) VALUES
(1, 'HEADING', 'Chapter 1: Number Systems', 1, 'Chapter 1: Number Systems', 1),
(1, 'TEXT', 'In this chapter, we will learn about different types of numbers and their properties. We will explore rational numbers, irrational numbers, and their representations on the number line.', 2, '', 1),
(1, 'TEXT', 'A number system is a way of representing numbers using digits or symbols. The most commonly used number system is the decimal system, which uses ten digits (0-9).', 3, 'Introduction to Number Systems', 2),
(2, 'HEADING', 'Chapter 2: Polynomials', 1, 'Chapter 2: Polynomials', 26),
(2, 'TEXT', 'Polynomials are algebraic expressions that consist of variables and coefficients. They are fundamental to understanding algebraic equations and functions.', 2, '', 26)
ON CONFLICT DO NOTHING;

-- Insert sample questions
INSERT INTO question_bank (chapter_id, question, options, correct_answer, explanation, difficulty, topic) VALUES
(1, 'What is the decimal representation of the fraction 2/3?', ARRAY['0.666', '0.666...', '0.66', '0.67'], 'B', '2/3 = 0.666... (repeating decimal)', 'EASY', 'Number Systems'),
(1, 'Which of the following is an irrational number?', ARRAY['√4', '√9', '√2', '1/2'], 'C', '√2 cannot be expressed as a fraction of two integers', 'MEDIUM', 'Irrational Numbers'),
(2, 'What is the degree of the polynomial x³ + 2x² - 5x + 1?', ARRAY['1', '2', '3', '4'], 'C', 'The degree is the highest power of x, which is 3', 'EASY', 'Polynomials'),
(2, 'If p(x) = x² - 4, what is p(2)?', ARRAY['0', '4', '8', '-4'], 'A', 'p(2) = (2)² - 4 = 4 - 4 = 0', 'EASY', 'Polynomial Evaluation')
ON CONFLICT DO NOTHING;

-- Create views for common queries
CREATE OR REPLACE VIEW book_chapters_summary AS
SELECT 
    b.id as book_id,
    b.class_grade,
    b.subject,
    b.title as book_title,
    COUNT(c.id) as chapter_count,
    MIN(c.start_page) as first_page,
    MAX(c.end_page) as last_page,
    b.total_pages
FROM ncert_books b
LEFT JOIN ncert_chapters c ON b.id = c.book_id
GROUP BY b.id, b.class_grade, b.subject, b.title, b.total_pages;

CREATE OR REPLACE VIEW chapter_content_summary AS
SELECT 
    c.id as chapter_id,
    c.title as chapter_title,
    c.chapter_number,
    b.class_grade,
    b.subject,
    b.title as book_title,
    COUNT(cb.id) as content_blocks_count,
    COUNT(CASE WHEN cb.block_type = 'TEXT' THEN 1 END) as text_blocks,
    COUNT(CASE WHEN cb.block_type = 'IMAGE' THEN 1 END) as image_blocks,
    COUNT(CASE WHEN cb.block_type = 'HEADING' THEN 1 END) as heading_blocks
FROM ncert_chapters c
JOIN ncert_books b ON c.book_id = b.id
LEFT JOIN content_blocks cb ON c.id = cb.chapter_id
GROUP BY c.id, c.title, c.chapter_number, b.class_grade, b.subject, b.title;

-- Create materialized view for question statistics (refresh manually or via cron)
CREATE MATERIALIZED VIEW IF NOT EXISTS question_statistics AS
SELECT 
    b.class_grade,
    b.subject,
    c.title as chapter_title,
    COUNT(q.id) as total_questions,
    COUNT(CASE WHEN q.difficulty = 'EASY' THEN 1 END) as easy_questions,
    COUNT(CASE WHEN q.difficulty = 'MEDIUM' THEN 1 END) as medium_questions,
    COUNT(CASE WHEN q.difficulty = 'HARD' THEN 1 END) as hard_questions,
    ARRAY_AGG(DISTINCT q.topic) FILTER (WHERE q.topic IS NOT NULL) as topics
FROM ncert_books b
JOIN ncert_chapters c ON b.id = c.book_id
LEFT JOIN question_bank q ON c.id = q.chapter_id
GROUP BY b.class_grade, b.subject, c.title;

-- Create index for materialized view
CREATE INDEX IF NOT EXISTS idx_question_statistics_class_subject ON question_statistics(class_grade, subject);

-- Grant permissions (adjust as needed for your setup)
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ncert_app_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ncert_app_user;

-- Create function to refresh materialized view
CREATE OR REPLACE FUNCTION refresh_question_statistics()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY question_statistics;
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON TABLE ncert_books IS 'Stores NCERT book information with GitHub integration';
COMMENT ON TABLE ncert_chapters IS 'Stores chapter information for each NCERT book';
COMMENT ON TABLE content_blocks IS 'Stores structured content blocks extracted from PDFs';
COMMENT ON TABLE question_bank IS 'Stores quiz questions with multiple choice options';
COMMENT ON TABLE user_sessions IS 'Tracks user sessions and preferences';
COMMENT ON TABLE ai_conversations IS 'Stores AI Q&A conversation history';
COMMENT ON TABLE quiz_results IS 'Stores quiz attempt results and statistics';
COMMENT ON TABLE audit_log IS 'Audit trail for all database changes';

-- Final verification queries
SELECT 'Database schema created successfully!' as status;
SELECT COUNT(*) as books_count FROM ncert_books;
SELECT COUNT(*) as chapters_count FROM ncert_chapters;
SELECT COUNT(*) as content_blocks_count FROM content_blocks;
SELECT COUNT(*) as questions_count FROM question_bank;
