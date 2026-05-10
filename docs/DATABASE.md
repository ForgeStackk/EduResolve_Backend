# NCERT Learning Platform - Database Documentation

## Overview

The NCERT Learning Platform uses PostgreSQL as its primary database. The database schema is designed to store NCERT book information, chapter content, quiz questions, and user interaction data.

## Database Schema

### Core Tables

#### 1. `ncert_books`
Stores information about NCERT books with GitHub integration.

**Columns:**
- `id` (BIGSERIAL, Primary Key) - Unique identifier
- `class_grade` (VARCHAR(50)) - Class level (e.g., "Class 9", "Class 10")
- `subject` (VARCHAR(100)) - Subject name (e.g., "Mathematics", "Physics")
- `title` (VARCHAR(500)) - Book title
- `github_url` (TEXT) - GitHub URL for the PDF
- `github_repo` (VARCHAR(200)) - GitHub repository name
- `github_path` (TEXT) - Path to PDF in repository
- `pdf_filename` (VARCHAR(255)) - PDF filename
- `total_pages` (INTEGER) - Total number of pages
- `uploaded_at` (TIMESTAMP) - Upload timestamp
- `updated_at` (TIMESTAMP) - Last update timestamp

**Indexes:**
- Unique constraint on (class_grade, subject, title)
- GIN index on title for full-text search
- B-tree indexes on class_grade and subject

#### 2. `ncert_chapters`
Stores chapter information for each book.

**Columns:**
- `id` (BIGSERIAL, Primary Key) - Unique identifier
- `book_id` (BIGINT, Foreign Key) - Reference to ncert_books
- `title` (VARCHAR(500)) - Chapter title
- `chapter_number` (INTEGER) - Chapter number
- `order_index` (INTEGER) - Display order
- `summary` (TEXT) - Chapter summary
- `start_page` (INTEGER) - Starting page number
- `end_page` (INTEGER) - Ending page number

**Relationships:**
- Many-to-one with ncert_books
- One-to-many with content_blocks and question_bank

#### 3. `content_blocks`
Stores structured content extracted from PDFs.

**Columns:**
- `id` (BIGSERIAL, Primary Key) - Unique identifier
- `chapter_id` (BIGINT, Foreign Key) - Reference to ncert_chapters
- `block_type` (ENUM) - Type: TEXT, IMAGE, DIAGRAM, TABLE, HEADING
- `content_text` (TEXT) - Text content
- `image_url` (TEXT) - URL for images
- `image_filename` (VARCHAR(255)) - Image filename
- `order_index` (INTEGER) - Display order
- `heading` (VARCHAR(500)) - Heading text
- `page_number` (INTEGER) - Page number

#### 4. `question_bank`
Stores quiz questions with multiple choice options.

**Columns:**
- `id` (BIGSERIAL, Primary Key) - Unique identifier
- `chapter_id` (BIGINT, Foreign Key) - Reference to ncert_chapters
- `question` (TEXT) - Question text
- `options` (TEXT[]) - Array of 4 options
- `correct_answer` (VARCHAR(10)) - Correct option (A, B, C, or D)
- `explanation` (TEXT) - Explanation for the answer
- `difficulty` (ENUM) - EASY, MEDIUM, HARD
- `topic` (VARCHAR(200)) - Topic category

### User Interaction Tables

#### 5. `user_sessions`
Tracks user sessions and preferences.

**Columns:**
- `id` (UUID, Primary Key) - Unique identifier
- `session_id` (VARCHAR(255), Unique) - Session identifier
- `class_level` (INTEGER) - User's class level (9-12)
- `subject` (VARCHAR(100)) - Preferred subject
- `created_at` (TIMESTAMP) - Session creation time
- `last_activity` (TIMESTAMP) - Last activity time
- `is_active` (BOOLEAN) - Session status

#### 6. `ai_conversations`
Stores AI Q&A conversation history.

**Columns:**
- `id` (UUID, Primary Key) - Unique identifier
- `session_id` (VARCHAR(255)) - Session identifier
- `class_level` (INTEGER) - Class level for context
- `subject` (VARCHAR(100)) - Subject context
- `question` (TEXT) - User's question
- `answer` (TEXT) - AI's response
- `context` (TEXT) - Additional context
- `created_at` (TIMESTAMP) - Conversation time

#### 7. `quiz_results`
Stores quiz attempt results and statistics.

**Columns:**
- `id` (UUID, Primary Key) - Unique identifier
- `session_id` (VARCHAR(255)) - Session identifier
- `book_id` (BIGINT) - Reference to ncert_books
- `class_level` (INTEGER) - Class level
- `subject` (VARCHAR(100)) - Subject
- `difficulty` (ENUM) - Quiz difficulty
- `total_questions` (INTEGER) - Number of questions
- `correct_answers` (INTEGER) - Correct answers count
- `percentage` (DECIMAL(5,2)) - Score percentage
- `time_spent_seconds` (INTEGER) - Time taken

### Audit Table

#### 8. `audit_log`
Tracks all database changes for auditing purposes.

**Columns:**
- `id` (BIGSERIAL, Primary Key) - Unique identifier
- `table_name` (VARCHAR(100)) - Table that was modified
- `record_id` (VARCHAR(100)) - ID of the record
- `operation` (VARCHAR(20)) - INSERT, UPDATE, DELETE
- `old_values` (JSONB) - Previous values (for UPDATE/DELETE)
- `new_values` (JSONB) - New values (for INSERT/UPDATE)
- `user_id` (VARCHAR(255)) - User who made the change
- `timestamp` (TIMESTAMP) - Change timestamp

## Database Views

### 1. `book_chapters_summary`
Provides a summary of books with their chapter counts.

```sql
SELECT * FROM book_chapters_summary;
```

### 2. `chapter_content_summary`
Shows chapter information with content block statistics.

```sql
SELECT * FROM chapter_content_summary;
```

### 3. `question_statistics` (Materialized View)
Aggregated question statistics by class, subject, and chapter.

```sql
SELECT * FROM question_statistics;

-- Refresh materialized view
SELECT refresh_question_statistics();
```

## Enum Types

### `block_type`
- `TEXT` - Text content blocks
- `IMAGE` - Image content
- `DIAGRAM` - Diagram illustrations
- `TABLE` - Table data
- `HEADING` - Section headings

### `difficulty_level`
- `EASY` - Simple questions
- `MEDIUM` - Moderate difficulty
- `HARD` - Challenging questions

## Database Functions

### `update_updated_at_column()`
Automatically updates the `updated_at` timestamp for modified records.

### `audit_trigger_function()`
Logs changes to audit tables for tracking modifications.

### `refresh_question_statistics()`
Refreshes the materialized view for question statistics.

## Triggers

1. **`update_ncert_books_updated_at`** - Updates timestamp on book modifications
2. **Audit Triggers** - Log changes to ncert_books, ncert_chapters, and content_blocks

## Indexes

### Performance Indexes
- Full-text search indexes on book titles, chapter titles, content text, and questions
- Composite indexes for common query patterns
- Foreign key indexes for join performance

### Search Optimization
```sql
-- Full-text search example
SELECT * FROM ncert_books 
WHERE to_tsvector('english', title || ' ' || subject) @@ to_tsquery('english', 'mathematics & class');

-- Search within content blocks
SELECT * FROM content_blocks 
WHERE to_tsvector('english', content_text) @@ to_tsquery('english', 'pythagorean');
```

## Sample Data

The database includes sample data for testing:
- 4 sample books (Class 9 & 10, Mathematics & Science)
- Sample chapters with content blocks
- Sample quiz questions with varying difficulty

## Migration Strategy

### Version 1.0
- Initial schema creation
- Basic tables and relationships
- Audit logging setup
- Sample data insertion

### Future Versions
- Add user authentication tables
- Implement bookmarking functionality
- Add progress tracking
- Enhance analytics capabilities

## Performance Considerations

### Connection Pooling
Configure connection pooling in `application.yaml`:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Query Optimization
- Use materialized views for complex aggregations
- Implement proper indexing for search queries
- Consider partitioning for large tables (content_blocks, question_bank)

### Caching
- Enable PostgreSQL query caching
- Implement application-level caching for frequently accessed data
- Use Redis for session management

## Backup and Recovery

### Backup Strategy
```bash
# Full backup
pg_dump -h localhost -U postgres -d ncert_learning > backup_$(date +%Y%m%d).sql

# Compressed backup
pg_dump -h localhost -U postgres -d ncert_learning | gzip > backup_$(date +%Y%m%d).sql.gz

# Restore
psql -h localhost -U postgres -d ncert_learning < backup_20231201.sql
```

### Point-in-Time Recovery
Configure WAL archiving for point-in-time recovery:
```sql
-- Enable WAL archiving in postgresql.conf
archive_mode = on
archive_command = 'cp %p /backup/archive/%f'
```

## Security Considerations

### Access Control
- Use least privilege principle for database users
- Implement row-level security for multi-tenant scenarios
- Encrypt sensitive data at rest and in transit

### Data Validation
- Check constraints ensure data integrity
- Foreign key constraints maintain referential integrity
- Enum types prevent invalid values

## Monitoring and Maintenance

### Regular Maintenance
```sql
-- Update table statistics
ANALYZE;

-- Rebuild indexes (if needed)
REINDEX DATABASE ncert_learning;

-- Clean up old audit logs
DELETE FROM audit_log WHERE timestamp < NOW() - INTERVAL '1 year';
```

### Performance Monitoring
- Monitor slow queries using `pg_stat_statements`
- Track connection pool usage
- Monitor disk space and growth patterns

## API Integration

### JPA Entity Mapping
The database schema maps to JPA entities:
- `NcertBook` → `ncert_books`
- `NcertChapter` → `ncert_chapters`
- `ContentBlock` → `content_blocks`
- `QuestionBank` → `question_bank`

### Repository Queries
Custom repository methods for complex queries:
```java
// Find books by class and subject
List<NcertBook> findByClassGradeAndSubject(String classGrade, String subject);

// Search content blocks
List<ContentBlock> findByContentTextContainingIgnoreCase(String searchText);

// Get questions by difficulty
List<QuestionBank> findByChapterIdAndDifficulty(Long chapterId, Difficulty difficulty);
```

## Troubleshooting

### Common Issues

1. **Connection Timeouts**
   - Check connection pool settings
   - Verify database server is running
   - Monitor connection counts

2. **Slow Queries**
   - Use `EXPLAIN ANALYZE` to analyze query plans
   - Check if indexes are being used
   - Consider query optimization

3. **Migration Failures**
   - Check PostgreSQL version compatibility
   - Verify required extensions are installed
   - Review migration script syntax

### Debug Queries
```sql
-- Check table sizes
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Check index usage
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

## Development Setup

### Local Development
1. Install PostgreSQL 13+
2. Create database: `CREATE DATABASE ncert_learning;`
3. Run migration: Apply `V1__Create_Ncert_Tables.sql`
4. Verify setup with sample queries

### Docker Setup
```dockerfile
FROM postgres:13
COPY src/main/resources/db/migration/V1__Create_Ncert_Tables.sql /docker-entrypoint-initdb.d/
ENV POSTGRES_DB=ncert_learning
ENV POSTGRES_USER=postgres
ENV POSTGRES_PASSWORD=password
```

This documentation provides a comprehensive guide to the NCERT Learning Platform database schema, maintenance procedures, and best practices.
