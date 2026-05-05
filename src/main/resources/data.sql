-- Seed data for EduResolve. Runs after Hibernate creates the schema (ddl-auto=update).
-- Idempotent: only insert when no rows exist.

INSERT INTO student_profile (user_id, name, initials, color, engagement, grade, status, class_name, streak_days, experience_points, top_percentage)
SELECT NULL, 'Marcus Thomas', 'MT', '#667eea', 92, 'A+', 'excellent', '10A', 12, 450, 3
WHERE NOT EXISTS (SELECT 1 FROM student_profile);

INSERT INTO student_profile (user_id, name, initials, color, engagement, grade, status, class_name, streak_days, experience_points, top_percentage)
SELECT NULL, 'Elena Vasquez', 'EV', '#764ba2', 78, 'B', 'good', '10A', 8, 320, 12
WHERE (SELECT COUNT(*) FROM student_profile) < 2;

INSERT INTO student_profile (user_id, name, initials, color, engagement, grade, status, class_name, streak_days, experience_points, top_percentage)
SELECT NULL, 'Julian Ross', 'JR', '#06b6d4', 45, 'D-', 'at-risk', '10A', 2, 90, 60
WHERE (SELECT COUNT(*) FROM student_profile) < 3;

INSERT INTO fee (student_id, student_name, class_name, phone, amount, due_date, status)
SELECT NULL, 'Sarah Johnson', 'Grade 10', '+1234567890', 2450.00, CURRENT_DATE + INTERVAL '15 days', 'Paid'
WHERE NOT EXISTS (SELECT 1 FROM fee);

INSERT INTO fee (student_id, student_name, class_name, phone, amount, due_date, status)
SELECT NULL, 'Michael Chen', 'Grade 10', '+1234567891', 2450.00, CURRENT_DATE + INTERVAL '5 days', 'Unpaid'
WHERE (SELECT COUNT(*) FROM fee) < 2;

INSERT INTO fee (student_id, student_name, class_name, phone, amount, due_date, status)
SELECT NULL, 'Emma Davis', 'Grade 9', '+1234567892', 1950.00, CURRENT_DATE + INTERVAL '7 days', 'Unpaid'
WHERE (SELECT COUNT(*) FROM fee) < 3;

INSERT INTO school_event (title, location, event_date, event_time, attendees_count)
SELECT 'Parent-Teacher Meet', 'Room 2048', CURRENT_DATE + INTERVAL '5 days', '4:30 PM', 2
WHERE NOT EXISTS (SELECT 1 FROM school_event);

INSERT INTO school_event (title, location, event_date, event_time, attendees_count)
SELECT 'Annual Science Fair 2025', 'Main Auditorium', CURRENT_DATE + INTERVAL '15 days', '09:00 AM', NULL
WHERE (SELECT COUNT(*) FROM school_event) < 2;

INSERT INTO quiz_question (subject, chapter, text, options_json, correct_option_id, explanation)
SELECT 'Science', 'Light - Reflection',
       'What is the speed of light in a vacuum?',
       '[{"id":"o1","text":"3 x 10^8 m/s"},{"id":"o2","text":"3 x 10^5 m/s"},{"id":"o3","text":"3 x 10^10 m/s"}]',
       'o1',
       'The speed of light in a vacuum is approximately 3 x 10^8 m/s.'
WHERE NOT EXISTS (SELECT 1 FROM quiz_question);

INSERT INTO quiz_question (subject, chapter, text, options_json, correct_option_id, explanation)
SELECT 'Science', 'Light - Reflection',
       'Which phenomenon explains the bending of light when passing between media?',
       '[{"id":"o1","text":"Reflection"},{"id":"o2","text":"Refraction"},{"id":"o3","text":"Dispersion"}]',
       'o2',
       'Refraction is the bending of light as it enters a different medium.'
WHERE (SELECT COUNT(*) FROM quiz_question) < 2;
