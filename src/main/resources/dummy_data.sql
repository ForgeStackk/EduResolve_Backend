-- =====================================================================
-- EduResolve - End-to-end test seed data
-- =====================================================================
-- Purpose: populate every entity table with rich, realistic dummy data
--          so you can exercise the full UI -> API -> DB -> UI flow.
--
-- Usage (DBeaver or psql):
--   \i dummy_data.sql                        -- from psql shell
--   psql -U postgres -d postgres -f dummy_data.sql
--
-- Safety:
--   * Each block is wrapped so it ONLY inserts if the table is empty for
--     that logical group. Re-running this script is a no-op once seeded.
--   * Order matters because some seed rows reference IDs from user_login.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. user_login  (admins, teachers, parents, students)
-- ---------------------------------------------------------------------
INSERT INTO user_login (name, class_name, email, password, role, phone_number)
SELECT * FROM (VALUES
    -- Admins
    ('Aarav Mehta',      NULL,  'admin@eduresolve.test',     'Admin@123',   'ADMIN',   '+91-9000000001'),
    -- Teachers
    ('Priya Sharma',     '10A', 'priya.t@eduresolve.test',   'Teach@123',   'TEACHER', '+91-9000000010'),
    ('Rohit Verma',      '10A', 'rohit.t@eduresolve.test',   'Teach@123',   'TEACHER', '+91-9000000011'),
    ('Anita Desai',      '12B', 'anita.t@eduresolve.test',   'Teach@123',   'TEACHER', '+91-9000000012'),
    -- Parents
    ('Sarah Johnson',    NULL,  'sarah.p@eduresolve.test',   'Parent@123',  'PARENT',  '+1-234-567-0001'),
    ('David Chen',       NULL,  'david.p@eduresolve.test',   'Parent@123',  'PARENT',  '+1-234-567-0002'),
    ('Olivia Davis',     NULL,  'olivia.p@eduresolve.test',  'Parent@123',  'PARENT',  '+1-234-567-0003'),
    ('Marcus Wilson',    NULL,  'marcus.p@eduresolve.test',  'Parent@123',  'PARENT',  '+1-234-567-0004'),
    -- Students
    ('Marcus Thomas',    '10A', 'marcus.s@eduresolve.test',  'Student@123', 'STUDENT', '+91-9000001001'),
    ('Elena Vasquez',    '10A', 'elena.s@eduresolve.test',   'Student@123', 'STUDENT', '+91-9000001002'),
    ('Julian Ross',      '10A', 'julian.s@eduresolve.test',  'Student@123', 'STUDENT', '+91-9000001003'),
    ('Nisha Patel',      '10B', 'nisha.s@eduresolve.test',   'Student@123', 'STUDENT', '+91-9000001004'),
    ('Arjun Singh',      '12A', 'arjun.s@eduresolve.test',   'Student@123', 'STUDENT', '+91-9000001005')
) AS v(name, class_name, email, password, role, phone_number)
WHERE NOT EXISTS (SELECT 1 FROM user_login WHERE email LIKE '%eduresolve.test');


-- ---------------------------------------------------------------------
-- 2. student_profile  (linked to student user_login rows by name)
-- ---------------------------------------------------------------------
INSERT INTO student_profile
    (user_id, name, initials, color, engagement, grade, status, class_name, streak_days, experience_points, top_percentage)
SELECT u.id, v.name, v.initials, v.color, v.engagement, v.grade, v.status, v.class_name, v.streak_days, v.xp, v.top_pct
FROM (VALUES
    ('Marcus Thomas', 'MT', '#667eea', 92, 'A+', 'excellent', '10A', 12, 450, 3),
    ('Elena Vasquez', 'EV', '#764ba2', 78, 'B',  'good',      '10A',  8, 320, 12),
    ('Julian Ross',   'JR', '#06b6d4', 45, 'D-', 'at-risk',   '10A',  2,  90, 60),
    ('Nisha Patel',   'NP', '#f59e0b', 85, 'A',  'excellent', '10B', 15, 510, 5),
    ('Arjun Singh',   'AS', '#10b981', 70, 'B+', 'good',      '12A',  6, 280, 18)
) AS v(name, initials, color, engagement, grade, status, class_name, streak_days, xp, top_pct)
LEFT JOIN user_login u ON u.name = v.name AND u.role = 'STUDENT'
WHERE NOT EXISTS (SELECT 1 FROM student_profile sp WHERE sp.name = v.name);


-- ---------------------------------------------------------------------
-- 3. homework
-- ---------------------------------------------------------------------
INSERT INTO homework (title, description, due_date, has_attachment, teacher_id, class_name, subject, created_at)
SELECT v.title, v.description, v.due_date, v.has_attachment,
       (SELECT id FROM user_login WHERE email = v.teacher_email),
       v.class_name, v.subject, NOW()
FROM (VALUES
    ('Algebra Worksheet 5',
     'Complete problems 1-20 from chapter 4. Show all working steps.',
     CURRENT_DATE + INTERVAL '3 days',  TRUE,  'priya.t@eduresolve.test', '10A', 'Mathematics'),
    ('Light & Reflection Lab Report',
     'Write up the experiment performed in class on concave mirrors. 2 pages.',
     CURRENT_DATE + INTERVAL '5 days',  FALSE, 'rohit.t@eduresolve.test', '10A', 'Science'),
    ('Essay: My Hero',
     '500-word essay on a personal hero. Submit as PDF.',
     CURRENT_DATE + INTERVAL '7 days',  FALSE, 'priya.t@eduresolve.test', '10A', 'English'),
    ('Calculus PYQ Set',
     'Solve the 2024 board paper integrals section.',
     CURRENT_DATE + INTERVAL '2 days',  TRUE,  'anita.t@eduresolve.test', '12B', 'Mathematics')
) AS v(title, description, due_date, has_attachment, teacher_email, class_name, subject)
WHERE NOT EXISTS (SELECT 1 FROM homework);


-- ---------------------------------------------------------------------
-- 4. doubt   (student questions; some answered, some pending)
-- ---------------------------------------------------------------------
INSERT INTO doubt (student_id, query, answer, is_helpful, subject, created_at)
SELECT (SELECT id FROM user_login WHERE email = v.student_email),
       v.query, v.answer, v.is_helpful, v.subject, NOW() - (v.minutes_ago * INTERVAL '1 minute')
FROM (VALUES
    ('marcus.s@eduresolve.test',
     'What is the difference between speed and velocity?',
     'Speed is a scalar quantity (magnitude only). Velocity is a vector (magnitude + direction).',
     TRUE,  'Science',     30),
    ('elena.s@eduresolve.test',
     'How do you factor a quadratic when the discriminant is negative?',
     'When b^2 - 4ac < 0, the quadratic has no real roots. You can still factor it over complex numbers.',
     TRUE,  'Mathematics', 75),
    ('julian.s@eduresolve.test',
     'Why does a magnet attract iron but not aluminium?',
     'Iron is ferromagnetic; its atomic structure allows magnetic domains to align. Aluminium is only weakly paramagnetic.',
     NULL,  'Science',    150),
    ('nisha.s@eduresolve.test',
     'Can you explain Snells law in one line?',
     NULL, NULL, 'Science', 5)
) AS v(student_email, query, answer, is_helpful, subject, minutes_ago)
WHERE NOT EXISTS (SELECT 1 FROM doubt);


-- ---------------------------------------------------------------------
-- 5. complaint   (parent tickets in various states)
-- ---------------------------------------------------------------------
INSERT INTO complaint (parent_id, category, subject, description, status, created_at, updated_at)
SELECT (SELECT id FROM user_login WHERE email = v.parent_email),
       v.category, v.subject, v.description, v.status,
       NOW() - (v.days_ago * INTERVAL '1 day'),
       NOW() - (v.days_ago * INTERVAL '1 day')
FROM (VALUES
    ('sarah.p@eduresolve.test',  'Transport',   'Bus route #128 delay',
     'School bus on route 128 has been arriving 25-30 minutes late for the past week.',
     'Pending',  3),
    ('david.p@eduresolve.test',  'Academics',   'Lab equipment shortage',
     'My son says the chemistry lab does not have enough beakers for each pair.',
     'InReview', 5),
    ('olivia.p@eduresolve.test', 'Billing',     'Duplicate fee charge',
     'I was charged the lab fee twice in the October invoice. Please refund.',
     'Resolved', 14),
    ('marcus.p@eduresolve.test', 'Facilities',  'Broken AC in classroom 12B',
     'The AC in 12B has been non-functional for 4 days.',
     'InReview', 2),
    ('sarah.p@eduresolve.test',  'Academics',   'Request for extra Math classes',
     'Could the school arrange remedial Math sessions for grade 10?',
     'Pending',  1)
) AS v(parent_email, category, subject, description, status, days_ago)
WHERE NOT EXISTS (SELECT 1 FROM complaint);


-- ---------------------------------------------------------------------
-- 6. fee
-- ---------------------------------------------------------------------
INSERT INTO fee (student_id, student_name, class_name, phone, amount, due_date, status, last_reminder_at)
SELECT (SELECT id FROM user_login WHERE name = v.student_name AND role = 'STUDENT'),
       v.student_name, v.class_name, v.phone, v.amount, v.due_date, v.status, v.last_reminder_at
FROM (VALUES
    ('Marcus Thomas',  '10A',     '+91-9000001001', 2450.00, CURRENT_DATE + INTERVAL '15 days', 'Paid',   NULL::timestamp),
    ('Elena Vasquez',  '10A',     '+91-9000001002', 2450.00, CURRENT_DATE + INTERVAL  '5 days', 'Unpaid', NOW() - INTERVAL '2 days'),
    ('Julian Ross',    '10A',     '+91-9000001003', 2450.00, CURRENT_DATE - INTERVAL  '3 days', 'Unpaid', NOW() - INTERVAL '1 day'),
    ('Nisha Patel',    '10B',     '+91-9000001004', 2450.00, CURRENT_DATE + INTERVAL '20 days', 'Paid',   NULL::timestamp),
    ('Arjun Singh',    '12A',     '+91-9000001005', 3200.00, CURRENT_DATE + INTERVAL  '7 days', 'Unpaid', NULL::timestamp)
) AS v(student_name, class_name, phone, amount, due_date, status, last_reminder_at)
WHERE NOT EXISTS (SELECT 1 FROM fee WHERE student_name = 'Marcus Thomas');


-- ---------------------------------------------------------------------
-- 7. notice  (admin broadcasts)
-- ---------------------------------------------------------------------
INSERT INTO notice (target_audience, message, channels, sent_at)
SELECT * FROM (VALUES
    ('All Parents (Grade 9-12)',
     'Reminder: Parent-Teacher meeting this Friday at 4:30 PM in Room 2048.',
     'whatsapp,sms',
     NOW() - INTERVAL '2 days'),
    ('Grade 10',
     'Mid-term exams begin next Monday. Hall ticket distribution at 9 AM tomorrow.',
     'whatsapp',
     NOW() - INTERVAL '1 day'),
    ('All Parents (Grade 9-12)',
     'School will remain closed on 26th October due to local elections.',
     'whatsapp,sms',
     NOW() - INTERVAL '6 hours'),
    ('Grade 12',
     'Career counselling session this Saturday in the auditorium. Mandatory for all.',
     'sms',
     NOW() - INTERVAL '3 hours')
) AS v(target_audience, message, channels, sent_at)
WHERE NOT EXISTS (SELECT 1 FROM notice);


-- ---------------------------------------------------------------------
-- 8. school_event
-- ---------------------------------------------------------------------
INSERT INTO school_event (title, location, event_date, event_time, attendees_count)
SELECT * FROM (VALUES
    ('Parent-Teacher Meet',           'Room 2048',           CURRENT_DATE + INTERVAL  '5 days',  '4:30 PM',   2),
    ('Annual Science Fair 2026',      'Main Auditorium',     CURRENT_DATE + INTERVAL '15 days', '09:00 AM',  NULL::int),
    ('Basketball Semifinals',         'Sports Complex',      CURRENT_DATE + INTERVAL '23 days', '03:00 PM',  NULL::int),
    ('Cultural Day',                  'Open Grounds',        CURRENT_DATE + INTERVAL '40 days', '10:00 AM',  NULL::int),
    ('Career Counselling Workshop',   'Auditorium',          CURRENT_DATE + INTERVAL  '3 days', '11:00 AM',  120),
    ('Inter-school Math Olympiad',    'Examination Hall',    CURRENT_DATE + INTERVAL '60 days', '09:30 AM',  NULL::int)
) AS v(title, location, event_date, event_time, attendees_count)
WHERE NOT EXISTS (SELECT 1 FROM school_event);


-- ---------------------------------------------------------------------
-- 9. quiz_question
-- ---------------------------------------------------------------------
INSERT INTO quiz_question (subject, chapter, text, options_json, correct_option_id, explanation)
SELECT * FROM (VALUES
    ('Science', 'Light - Reflection',
     'What is the speed of light in a vacuum?',
     '[{"id":"o1","text":"3 x 10^8 m/s"},{"id":"o2","text":"3 x 10^5 m/s"},{"id":"o3","text":"3 x 10^10 m/s"}]',
     'o1',
     'The speed of light in a vacuum is approximately 3 x 10^8 m/s.'),
    ('Science', 'Light - Reflection',
     'Which phenomenon explains the bending of light when passing between media?',
     '[{"id":"o1","text":"Reflection"},{"id":"o2","text":"Refraction"},{"id":"o3","text":"Dispersion"}]',
     'o2',
     'Refraction is the bending of light as it enters a different medium.'),
    ('Science', 'Motion',
     'Newtons first law is also known as the law of?',
     '[{"id":"o1","text":"Inertia"},{"id":"o2","text":"Acceleration"},{"id":"o3","text":"Action-Reaction"}]',
     'o1',
     'An object remains at rest or in uniform motion unless acted upon by an external force.'),
    ('Mathematics', 'Algebra',
     'Solve for x: 3x + 5 = 20',
     '[{"id":"o1","text":"3"},{"id":"o2","text":"5"},{"id":"o3","text":"15"}]',
     'o2',
     '3x = 20 - 5 = 15, therefore x = 5.'),
    ('Mathematics', 'Geometry',
     'The sum of interior angles of a triangle equals?',
     '[{"id":"o1","text":"90 deg"},{"id":"o2","text":"180 deg"},{"id":"o3","text":"360 deg"}]',
     'o2',
     'For any planar triangle, the interior angles sum to 180 degrees.'),
    ('Mathematics', 'Calculus',
     'Derivative of sin(x) with respect to x is?',
     '[{"id":"o1","text":"cos(x)"},{"id":"o2","text":"-cos(x)"},{"id":"o3","text":"-sin(x)"}]',
     'o1',
     'd/dx [sin(x)] = cos(x).'),
    ('English', 'Grammar',
     'Choose the correct article: "She is ___ honest person."',
     '[{"id":"o1","text":"a"},{"id":"o2","text":"an"},{"id":"o3","text":"the"}]',
     'o2',
     'Honest begins with a silent h, so the vowel sound takes "an".'),
    ('English', 'Literature',
     'Who wrote "Romeo and Juliet"?',
     '[{"id":"o1","text":"Charles Dickens"},{"id":"o2","text":"William Shakespeare"},{"id":"o3","text":"Jane Austen"}]',
     'o2',
     'Shakespeare wrote the tragedy in the early part of his career, around 1594-96.')
) AS v(subject, chapter, text, options_json, correct_option_id, explanation)
WHERE NOT EXISTS (SELECT 1 FROM quiz_question WHERE chapter = 'Motion');


COMMIT;

-- =====================================================================
-- Verification: row counts per table
-- =====================================================================
SELECT 'user_login'      AS table_name, COUNT(*) AS row_count FROM user_login
UNION ALL SELECT 'student_profile', COUNT(*) FROM student_profile
UNION ALL SELECT 'homework',        COUNT(*) FROM homework
UNION ALL SELECT 'doubt',           COUNT(*) FROM doubt
UNION ALL SELECT 'complaint',       COUNT(*) FROM complaint
UNION ALL SELECT 'fee',             COUNT(*) FROM fee
UNION ALL SELECT 'notice',          COUNT(*) FROM notice
UNION ALL SELECT 'school_event',    COUNT(*) FROM school_event
UNION ALL SELECT 'quiz_question',   COUNT(*) FROM quiz_question
ORDER BY table_name;
