-- Denormalise school_name onto classroom so we can enforce same-school checks
-- without a 3-table join every time.
-- Source of truth: user_login.school_name of the class teacher.

ALTER TABLE classroom ADD COLUMN school_name VARCHAR(200);

UPDATE classroom cr
SET    school_name = ul.school_name
FROM   teacher   t
JOIN   user_login ul ON ul.id = t.user_id
WHERE  t.teacher_id = cr.class_teacher_id;

-- Classrooms without a class-teacher get the school_name of any teacher
-- mapped to teach in that class (fallback for classrooms created before V3).
UPDATE classroom cr
SET    school_name = ul.school_name
FROM   teacher_subject_mapping tsm
JOIN   teacher   t  ON t.teacher_id = tsm.teacher_id
JOIN   user_login ul ON ul.id = t.user_id
WHERE  cr.class_id    = tsm.class_id
  AND  cr.school_name IS NULL;

CREATE INDEX idx_classroom_school_name ON classroom (school_name);
