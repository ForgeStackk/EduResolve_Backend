-- ================================================================
-- EduResolve — Teacher Portal Test Seed Data
-- ================================================================
-- How to run (pick one):
--   psql -U <user> -d <db> -f seed-teacher-portal.sql
--   OR paste into DBeaver / pgAdmin query window
--
-- Pre-condition:
--   At least ONE user_login row with role = 'teacher' must exist.
--   Register one via POST /api/auth/register or the sign-up page.
--
-- Safe to re-run: every insert uses ON CONFLICT DO NOTHING.
-- ================================================================

DO $$
DECLARE
  -- ── resolved at runtime ──────────────────────────────────────
  v_user_id    BIGINT;
  v_teacher_id UUID;

  -- ── fixed UUIDs so re-runs are idempotent ────────────────────
  c_class_9a   CONSTANT UUID := '11110001-0000-0000-0000-000000000001';
  c_class_10b  CONSTANT UUID := '11110002-0000-0000-0000-000000000002';

  c_parent1    CONSTANT UUID := 'aa000001-0000-0000-0000-000000000001';
  c_parent2    CONSTANT UUID := 'aa000002-0000-0000-0000-000000000002';
  c_parent3    CONSTANT UUID := 'aa000003-0000-0000-0000-000000000003';
  c_parent4    CONSTANT UUID := 'aa000004-0000-0000-0000-000000000004';

  c_s1         CONSTANT UUID := 'bb000001-0000-0000-0000-000000000001';
  c_s2         CONSTANT UUID := 'bb000002-0000-0000-0000-000000000002';
  c_s3         CONSTANT UUID := 'bb000003-0000-0000-0000-000000000003';
  c_s4         CONSTANT UUID := 'bb000004-0000-0000-0000-000000000004';
  c_s5         CONSTANT UUID := 'bb000005-0000-0000-0000-000000000005';
  c_s6         CONSTANT UUID := 'bb000006-0000-0000-0000-000000000006';
  c_s7         CONSTANT UUID := 'bb000007-0000-0000-0000-000000000007';
  c_s8         CONSTANT UUID := 'bb000008-0000-0000-0000-000000000008';
  c_s9         CONSTANT UUID := 'bb000009-0000-0000-0000-000000000009';

  c_msg1       CONSTANT UUID := 'dd000001-0000-0000-0000-000000000001';
  c_msg2       CONSTANT UUID := 'dd000002-0000-0000-0000-000000000002';
  c_msg3       CONSTANT UUID := 'dd000003-0000-0000-0000-000000000003';
  c_msg4       CONSTANT UUID := 'dd000004-0000-0000-0000-000000000004';
  c_msg5       CONSTANT UUID := 'dd000005-0000-0000-0000-000000000005';

  c_report1    CONSTANT UUID := 'ee000001-0000-0000-0000-000000000001';
  c_report2    CONSTANT UUID := 'ee000002-0000-0000-0000-000000000002';

BEGIN

  -- ================================================================
  -- 0. Resolve teacher user
  -- ================================================================
  SELECT id INTO v_user_id
  FROM user_login
  WHERE LOWER(role) = 'teacher'
  ORDER BY id
  LIMIT 1;

  IF v_user_id IS NULL THEN
    RAISE EXCEPTION
      E'No teacher account found.\n'
      'Register one via POST /api/auth/register with role="teacher" first.';
  END IF;

  RAISE NOTICE '→ Using user_login.id = %', v_user_id;

  -- ================================================================
  -- 1. Teacher portal record (teacher table, not legacy teacher_profile)
  -- ================================================================
  INSERT INTO teacher (full_name, email, user_id, status)
  SELECT
    NULLIF(TRIM(COALESCE(first_name,'') || ' ' || COALESCE(last_name,'')), ''),
    email,
    id,
    'ACTIVE'
  FROM user_login
  WHERE id = v_user_id
    AND NOT EXISTS (SELECT 1 FROM teacher WHERE user_id = v_user_id);

  -- If teacher row already existed (bootstrapped on prior login), just read it
  SELECT teacher_id INTO v_teacher_id
  FROM teacher
  WHERE user_id = v_user_id;

  IF v_teacher_id IS NULL THEN
    RAISE EXCEPTION 'Could not create or find teacher record for user_id=%', v_user_id;
  END IF;

  RAISE NOTICE '→ teacher_id = %', v_teacher_id;

  -- ================================================================
  -- 2. Classrooms (insert without CT first to avoid circular FK)
  -- ================================================================
  INSERT INTO classroom (class_id, class_name, section, academic_year)
  VALUES
    (c_class_9a,  'Class 9',  'A', '2025-26'),
    (c_class_10b, 'Class 10', 'B', '2025-26')
  ON CONFLICT DO NOTHING;

  RAISE NOTICE '→ Classrooms ready.';

  -- ================================================================
  -- 3. Wire teacher ↔ classroom (Class 9-A is the CT class)
  -- ================================================================
  UPDATE teacher   SET class_teacher_of  = c_class_9a   WHERE teacher_id = v_teacher_id;
  UPDATE classroom SET class_teacher_id  = v_teacher_id WHERE class_id   = c_class_9a;

  RAISE NOTICE '→ Class-teacher links set.';

  -- ================================================================
  -- 4. Subject mappings
  --    subject_id has no FK — 1/2/3 are placeholder IDs.
  --    Fixed 'id' UUIDs so re-runs skip duplicates via PK conflict.
  -- ================================================================
  INSERT INTO teacher_subject_mapping (id, teacher_id, class_id, subject_id)
  VALUES
    ('99000001-0000-0000-0000-000000000001', v_teacher_id, c_class_9a,  1),  -- Maths  9-A
    ('99000002-0000-0000-0000-000000000002', v_teacher_id, c_class_9a,  2),  -- Science 9-A
    ('99000003-0000-0000-0000-000000000003', v_teacher_id, c_class_10b, 2),  -- Science 10-B
    ('99000004-0000-0000-0000-000000000004', v_teacher_id, c_class_10b, 3)   -- English 10-B
  ON CONFLICT DO NOTHING;

  RAISE NOTICE '→ Subject mappings done.';

  -- ================================================================
  -- 5. Parents
  -- ================================================================
  INSERT INTO tp_parent (parent_id, full_name, email, phone)
  VALUES
    (c_parent1, 'Ramesh Sharma',  'ramesh.sharma@school.test',  '9876543210'),
    (c_parent2, 'Sunita Verma',   'sunita.verma@school.test',   '9876543211'),
    (c_parent3, 'Mohan Gupta',    'mohan.gupta@school.test',    '9876543212'),
    (c_parent4, 'Anita Yadav',    'anita.yadav@school.test',    '9876543213')
  ON CONFLICT DO NOTHING;

  RAISE NOTICE '→ Parents inserted.';

  -- ================================================================
  -- 6. Students — Class 9-A (CT class, 5 students)
  -- ================================================================
  INSERT INTO tp_student (student_id, full_name, roll_number, class_id, parent_id, status)
  VALUES
    (c_s1, 'Arjun Sharma',   '01', c_class_9a, c_parent1, 'ACTIVE'),
    (c_s2, 'Priya Verma',    '02', c_class_9a, c_parent2, 'ACTIVE'),
    (c_s3, 'Rohan Gupta',    '03', c_class_9a, c_parent3, 'ACTIVE'),
    (c_s4, 'Anjali Singh',   '04', c_class_9a, c_parent4, 'ACTIVE'),
    (c_s5, 'Vikram Patel',   '05', c_class_9a, NULL,      'ACTIVE')
  ON CONFLICT DO NOTHING;

  -- Class 10-B (subject class, 4 students)
  INSERT INTO tp_student (student_id, full_name, roll_number, class_id, parent_id, status)
  VALUES
    (c_s6, 'Neha Yadav',    '01', c_class_10b, NULL, 'ACTIVE'),
    (c_s7, 'Kiran Mishra',  '02', c_class_10b, NULL, 'ACTIVE'),
    (c_s8, 'Deepak Kumar',  '03', c_class_10b, NULL, 'ACTIVE'),
    (c_s9, 'Meena Joshi',   '04', c_class_10b, NULL, 'ACTIVE')
  ON CONFLICT DO NOTHING;

  RAISE NOTICE '→ Students inserted (9 total).';

  -- ================================================================
  -- 7. Attendance — last 5 school days for Class 9-A
  --    Statuses: PRESENT | ABSENT | LATE | HALF_DAY | HOLIDAY
  -- ================================================================
  INSERT INTO attendance (class_id, student_id, date, status, marked_by, remarks)
  VALUES
    -- Day 0 (today)
    (c_class_9a, c_s1, CURRENT_DATE,     'PRESENT',  v_teacher_id, NULL),
    (c_class_9a, c_s2, CURRENT_DATE,     'PRESENT',  v_teacher_id, NULL),
    (c_class_9a, c_s3, CURRENT_DATE,     'ABSENT',   v_teacher_id, 'Sick leave'),
    (c_class_9a, c_s4, CURRENT_DATE,     'PRESENT',  v_teacher_id, NULL),
    (c_class_9a, c_s5, CURRENT_DATE,     'LATE',     v_teacher_id, 'Bus delay — 15 min'),

    -- Day -1
    (c_class_9a, c_s1, CURRENT_DATE - 1, 'PRESENT',  v_teacher_id, NULL),
    (c_class_9a, c_s2, CURRENT_DATE - 1, 'ABSENT',   v_teacher_id, 'Family function'),
    (c_class_9a, c_s3, CURRENT_DATE - 1, 'PRESENT',  v_teacher_id, NULL),
    (c_class_9a, c_s4, CURRENT_DATE - 1, 'PRESENT',  v_teacher_id, NULL),
    (c_class_9a, c_s5, CURRENT_DATE - 1, 'PRESENT',  v_teacher_id, NULL),

    -- Day -2
    (c_class_9a, c_s1, CURRENT_DATE - 2, 'PRESENT',  v_teacher_id, NULL),
    (c_class_9a, c_s2, CURRENT_DATE - 2, 'PRESENT',  v_teacher_id, NULL),
    (c_class_9a, c_s3, CURRENT_DATE - 2, 'LATE',     v_teacher_id, 'Doctor appointment'),
    (c_class_9a, c_s4, CURRENT_DATE - 2, 'ABSENT',   v_teacher_id, NULL),
    (c_class_9a, c_s5, CURRENT_DATE - 2, 'PRESENT',  v_teacher_id, NULL),

    -- Day -3
    (c_class_9a, c_s1, CURRENT_DATE - 3, 'PRESENT',  v_teacher_id, NULL),
    (c_class_9a, c_s2, CURRENT_DATE - 3, 'PRESENT',  v_teacher_id, NULL),
    (c_class_9a, c_s3, CURRENT_DATE - 3, 'PRESENT',  v_teacher_id, NULL),
    (c_class_9a, c_s4, CURRENT_DATE - 3, 'PRESENT',  v_teacher_id, NULL),
    (c_class_9a, c_s5, CURRENT_DATE - 3, 'ABSENT',   v_teacher_id, NULL),

    -- Day -4
    (c_class_9a, c_s1, CURRENT_DATE - 4, 'PRESENT',  v_teacher_id, NULL),
    (c_class_9a, c_s2, CURRENT_DATE - 4, 'PRESENT',  v_teacher_id, NULL),
    (c_class_9a, c_s3, CURRENT_DATE - 4, 'PRESENT',  v_teacher_id, NULL),
    (c_class_9a, c_s4, CURRENT_DATE - 4, 'HALF_DAY', v_teacher_id, 'Left early'),
    (c_class_9a, c_s5, CURRENT_DATE - 4, 'PRESENT',  v_teacher_id, NULL)

  ON CONFLICT ON CONSTRAINT uq_attendance_class_student_date DO NOTHING;

  RAISE NOTICE '→ Attendance records inserted.';

  -- ================================================================
  -- 8. Attendance reports
  --    Current month + previous month (for archive testing)
  -- ================================================================
  INSERT INTO attendance_report
    (report_id, class_id, month, year, generated_by, status, summary)
  VALUES
  (
    c_report1,
    c_class_9a,
    EXTRACT(MONTH FROM CURRENT_DATE)::INTEGER,
    EXTRACT(YEAR  FROM CURRENT_DATE)::INTEGER,
    'TEACHER', 'GENERATED',
    jsonb_build_object(
      'totalWorkingDays', 20,
      'studentWiseSummary', jsonb_build_array(
        jsonb_build_object('studentId', c_s1::text, 'name', 'Arjun Sharma',  'presentDays', 19, 'absentDays', 1, 'lateDays', 0),
        jsonb_build_object('studentId', c_s2::text, 'name', 'Priya Verma',   'presentDays', 17, 'absentDays', 3, 'lateDays', 0),
        jsonb_build_object('studentId', c_s3::text, 'name', 'Rohan Gupta',   'presentDays', 16, 'absentDays', 2, 'lateDays', 2),
        jsonb_build_object('studentId', c_s4::text, 'name', 'Anjali Singh',  'presentDays', 18, 'absentDays', 2, 'lateDays', 0),
        jsonb_build_object('studentId', c_s5::text, 'name', 'Vikram Patel',  'presentDays', 17, 'absentDays', 2, 'lateDays', 1)
      )
    )
  ),
  (
    c_report2,
    c_class_9a,
    -- Previous month (wraps year if current month is January)
    CASE WHEN EXTRACT(MONTH FROM CURRENT_DATE) = 1 THEN 12
         ELSE EXTRACT(MONTH FROM CURRENT_DATE)::INTEGER - 1 END,
    CASE WHEN EXTRACT(MONTH FROM CURRENT_DATE) = 1 THEN EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER - 1
         ELSE EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER END,
    'TEACHER', 'SENT_TO_PARENTS',
    jsonb_build_object(
      'totalWorkingDays', 22,
      'studentWiseSummary', jsonb_build_array(
        jsonb_build_object('studentId', c_s1::text, 'name', 'Arjun Sharma',  'presentDays', 22, 'absentDays', 0, 'lateDays', 0),
        jsonb_build_object('studentId', c_s2::text, 'name', 'Priya Verma',   'presentDays', 20, 'absentDays', 2, 'lateDays', 0),
        jsonb_build_object('studentId', c_s3::text, 'name', 'Rohan Gupta',   'presentDays', 19, 'absentDays', 1, 'lateDays', 2),
        jsonb_build_object('studentId', c_s4::text, 'name', 'Anjali Singh',  'presentDays', 21, 'absentDays', 1, 'lateDays', 0),
        jsonb_build_object('studentId', c_s5::text, 'name', 'Vikram Patel',  'presentDays', 18, 'absentDays', 3, 'lateDays', 1)
      )
    )
  )
  ON CONFLICT DO NOTHING;

  RAISE NOTICE '→ Attendance reports inserted (current + previous month).';

  -- ================================================================
  -- 9. Messages (CLASS announcements + homework dispatch)
  -- ================================================================
  INSERT INTO message
    (message_id, sender_id, sender_role, recipient_type,
     target_class_id, content_type, text_body, is_homework, homework_due_date)
  VALUES
    (
      c_msg1, v_teacher_id, 'TEACHER', 'CLASS',
      c_class_9a, 'TEXT',
      'Dear parents, please ensure your ward completes Chapter 5 exercises by this Friday.',
      false, NULL
    ),
    (
      c_msg2, v_teacher_id, 'TEACHER', 'CLASS',
      c_class_9a, 'TEXT',
      'Homework: Solve Q1–Q10 from Exercise 3.2 (Linear Equations). Due: ' ||
        TO_CHAR(CURRENT_DATE + 3, 'DD Mon YYYY') || '.',
      true, CURRENT_DATE + 3
    ),
    (
      c_msg3, v_teacher_id, 'TEACHER', 'CLASS',
      c_class_10b, 'TEXT',
      'Science unit test on Monday. Syllabus: Chapters 1–4 (Matter, Forces, Light, Sound).',
      false, NULL
    ),
    (
      c_msg4, v_teacher_id, 'TEACHER', 'CLASS',
      c_class_9a, 'TEXT',
      'PTM (Parent–Teacher Meeting) scheduled for Saturday 10 AM – 1 PM. All parents must attend.',
      false, NULL
    ),
    (
      c_msg5, v_teacher_id, 'TEACHER', 'CLASS',
      c_class_9a, 'TEXT',
      'Homework: Write a short essay (200 words) on "Water Conservation". Due: ' ||
        TO_CHAR(CURRENT_DATE + 5, 'DD Mon YYYY') || '.',
      true, CURRENT_DATE + 5
    )
  ON CONFLICT DO NOTHING;

  RAISE NOTICE '→ Messages inserted (3 announcements + 2 homework).';

  -- ================================================================
  -- 10. Teacher notifications (unread + read mix)
  -- ================================================================
  INSERT INTO teacher_notification (notification_id, teacher_id, message, is_read)
  VALUES
    ('ff000001-0000-0000-0000-000000000001',
     v_teacher_id,
     'Rohan Gupta has been absent 3 days this week in Class 9-A',    false),
    ('ff000002-0000-0000-0000-000000000002',
     v_teacher_id,
     'Vikram Patel arrived late today — remarks logged',              false),
    ('ff000003-0000-0000-0000-000000000003',
     v_teacher_id,
     'Attendance report for ' || TO_CHAR(CURRENT_DATE, 'Month YYYY') || ' generated successfully', true),
    ('ff000004-0000-0000-0000-000000000004',
     v_teacher_id,
     'Parent Ramesh Sharma read your message at 09:14 AM',           true),
    ('ff000005-0000-0000-0000-000000000005',
     v_teacher_id,
     'Homework dispatched to 5 students in Class 9-A',               true)
  ON CONFLICT DO NOTHING;

  RAISE NOTICE '→ Notifications inserted.';

  -- ================================================================
  -- Summary
  -- ================================================================
  RAISE NOTICE '';
  RAISE NOTICE '╔══════════════════════════════════════════════════╗';
  RAISE NOTICE '║  Seed complete — IDs to keep handy              ║';
  RAISE NOTICE '╠══════════════════════════════════════════════════╣';
  RAISE NOTICE '║  teacher_id : %  ║', v_teacher_id;
  RAISE NOTICE '║  Class 9-A  : 11110001-0000-0000-0000-000000000001 ║';
  RAISE NOTICE '║  Class 10-B : 11110002-0000-0000-0000-000000000002 ║';
  RAISE NOTICE '╚══════════════════════════════════════════════════╝';

END $$;
