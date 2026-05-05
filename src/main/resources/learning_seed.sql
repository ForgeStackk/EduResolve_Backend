-- =====================================================================
-- EduResolve - Learning content seed data (subjects -> chapters -> topics
-- -> content_chunk + quiz_question + previous_year_question)
-- =====================================================================
-- Hibernate auto-creates the tables. This file populates them with a
-- realistic minimum for grades 9-12.  Run it as a separate script if you
-- prefer not to merge with data.sql.
-- =====================================================================

BEGIN;

-- 1. Subjects -----------------------------------------------------------
INSERT INTO subject (name, grade, icon, color_hex)
SELECT * FROM (VALUES
    ('Physics',     '10', '⚡',  '#667eea'),
    ('Mathematics', '10', '∑',   '#764ba2'),
    ('Science',     '10', '🧪', '#10b981'),
    ('English',     '10', '📖', '#3b82f6'),
    ('Physics',     '12', '⚡',  '#667eea'),
    ('Mathematics', '12', '∑',   '#764ba2')
) AS v(name, grade, icon, color_hex)
WHERE NOT EXISTS (SELECT 1 FROM subject);

-- 2. Chapters -----------------------------------------------------------
INSERT INTO chapter (subject_id, name, order_index, summary, estimated_minutes)
SELECT s.id, v.name, v.idx, v.summary, v.mins
FROM (VALUES
    ('Physics',     '10', 'Light - Reflection',         1, 'Laws of reflection, plane and curved mirrors.',      45),
    ('Physics',     '10', 'Electricity',                2, 'Ohms law, resistors in series and parallel.',        50),
    ('Physics',     '10', 'Magnetic Effects of Current',3, 'Right-hand rule, electromagnetic induction.',        40),
    ('Mathematics', '10', 'Real Numbers',               1, 'Euclids lemma, irrational numbers.',                 35),
    ('Mathematics', '10', 'Polynomials',                2, 'Zeros and coefficients, division algorithm.',        45),
    ('Mathematics', '10', 'Triangles',                  3, 'Similarity, theorems, Pythagoras.',                  60),
    ('Science',     '10', 'Acids, Bases and Salts',     1, 'pH scale, indicators, neutralisation.',              40),
    ('Science',     '10', 'Life Processes',             2, 'Nutrition, respiration, transportation.',            50),
    ('English',     '10', 'First Flight - The Letter',  1, 'Theme, characters and central idea.',                30),
    ('Physics',     '12', 'Electrostatics',             1, 'Coulombs law, electric fields, potentials.',         55),
    ('Mathematics', '12', 'Integrals',                  1, 'Definite & indefinite integrals, methods.',          70)
) AS v(subject_name, grade, name, idx, summary, mins)
JOIN subject s ON s.name = v.subject_name AND s.grade = v.grade
WHERE NOT EXISTS (SELECT 1 FROM chapter);

-- 3. Topics -------------------------------------------------------------
INSERT INTO topic (chapter_id, name, order_index, summary)
SELECT c.id, v.name, v.idx, v.summary
FROM (VALUES
    ('Light - Reflection', 'Plane Mirrors',                1, 'Reflection from flat surfaces; image properties.'),
    ('Light - Reflection', 'Concave & Convex Mirrors',     2, 'Curved mirrors; mirror formula and magnification.'),
    ('Electricity',        'Ohms Law',                     1, 'V = IR; verifying with experiments.'),
    ('Electricity',        'Series & Parallel Circuits',   2, 'Equivalent resistance and current sharing.'),
    ('Real Numbers',       'Euclids Division Lemma',       1, 'Statement, proof and applications.'),
    ('Polynomials',        'Zeros of a Polynomial',        1, 'Geometric meaning of zeros.'),
    ('Triangles',          'Similarity Criteria',          1, 'AAA, SAS, SSS for similar triangles.'),
    ('Acids, Bases and Salts', 'pH Scale',                 1, 'Concept of pH and indicator behaviour.'),
    ('Life Processes',     'Human Digestive System',       1, 'Organs, enzymes, absorption.'),
    ('Integrals',          'Methods of Integration',       1, 'Substitution and parts.')
) AS v(chapter_name, name, idx, summary)
JOIN chapter c ON c.name = v.chapter_name
WHERE NOT EXISTS (SELECT 1 FROM topic);

-- 4. ContentChunk - English -------------------------------------------
INSERT INTO content_chunk (topic_id, chunk_type, language, title, body, order_index, source, created_at)
SELECT t.id, v.chunk_type::varchar, 'en', v.title, v.body, v.idx, 'seed', NOW()
FROM (VALUES
    ('Plane Mirrors',           'SUMMARY',      'Quick Summary',
     'A plane mirror produces an image that is upright, virtual, of the same size as the object, and laterally inverted. The image is formed at the same distance behind the mirror as the object is in front.',
     1),
    ('Plane Mirrors',           'EXPLANATION',  'Detailed Explanation',
     'Light rays striking a plane mirror obey the two laws of reflection: (1) the angle of incidence equals the angle of reflection, and (2) the incident ray, the reflected ray, and the normal all lie in the same plane. The image appears to come from a point behind the mirror because our eye/brain extrapolates the reflected rays backward.',
     2),
    ('Plane Mirrors',           'EXAMPLE',      'Worked Example',
     'A 1.5 m tall person stands 2 m in front of a plane mirror. The image is 1.5 m tall, virtual, and located 2 m behind the mirror. Total perceived distance to the image = 4 m.',
     3),
    ('Concave & Convex Mirrors','SUMMARY',      'Quick Summary',
     'Concave mirrors converge light and can form real or virtual images. Convex mirrors always form virtual, diminished, and upright images.',
     1),
    ('Ohms Law',                'EXPLANATION',  'Statement & Derivation',
     'At constant temperature, the current flowing through a conductor is directly proportional to the potential difference across it: V = IR, where R is the resistance.',
     1),
    ('Ohms Law',                'EXAMPLE',      'Numerical',
     'A 12 V battery drives 2 A through a resistor. Resistance R = V/I = 6 ohm.',
     2),
    ('pH Scale',                'SUMMARY',      'Quick Summary',
     'pH measures the hydrogen-ion concentration of a solution on a scale of 0-14. pH < 7 is acidic, pH = 7 is neutral, pH > 7 is basic.',
     1),
    ('Human Digestive System',  'SUMMARY',      'Quick Summary',
     'Digestion converts complex food into absorbable molecules through mouth -> oesophagus -> stomach -> small intestine -> large intestine. Enzymes such as amylase, pepsin and trypsin do most of the chemical work.',
     1)
) AS v(topic_name, chunk_type, title, body, idx)
JOIN topic t ON t.name = v.topic_name
WHERE NOT EXISTS (SELECT 1 FROM content_chunk WHERE language = 'en');

-- 5. ContentChunk - Hindi (a small subset to demo i18n) ----------------
INSERT INTO content_chunk (topic_id, chunk_type, language, title, body, order_index, source, created_at)
SELECT t.id, v.chunk_type::varchar, 'hi', v.title, v.body, v.idx, 'seed', NOW()
FROM (VALUES
    ('Plane Mirrors', 'SUMMARY', 'सारांश',
     'समतल दर्पण से बना प्रतिबिम्ब आभासी, सीधा, समान आकार का तथा पार्श्विक रूप से उल्टा होता है। प्रतिबिम्ब वस्तु से उतनी ही दूरी पर दर्पण के पीछे बनता है।',
     1),
    ('Ohms Law', 'EXPLANATION', 'ओम का नियम',
     'स्थिर तापमान पर किसी चालक से बहती धारा उसके सिरों पर लगाए विभवांतर के समानुपाती होती है: V = IR.',
     1),
    ('pH Scale', 'SUMMARY', 'pH पैमाना',
     'pH विलयन में हाइड्रोजन आयन की सांद्रता का पैमाना है (0-14)। pH < 7 अम्लीय, 7 उदासीन, > 7 क्षारीय।',
     1)
) AS v(topic_name, chunk_type, title, body, idx)
JOIN topic t ON t.name = v.topic_name
WHERE NOT EXISTS (SELECT 1 FROM content_chunk WHERE language = 'hi');

-- 6. quiz_question - link existing seeded English quiz rows to chapter ids
UPDATE quiz_question
   SET chapter_id = (SELECT id FROM chapter WHERE name = quiz_question.chapter LIMIT 1),
       difficulty = COALESCE(difficulty, 'MEDIUM'),
       language   = COALESCE(language,   'en')
 WHERE chapter_id IS NULL;

-- Add a few mixed-difficulty questions tied to chapter ids
INSERT INTO quiz_question (subject, chapter, chapter_id, topic_id, difficulty, language, text, options_json, correct_option_id, explanation)
SELECT v.subject, v.chapter, c.id, NULL, v.difficulty, 'en', v.text, v.options_json, v.correct, v.explanation
FROM (VALUES
    ('Physics',     'Light - Reflection', 'EASY',
     'Which of these mirrors is used in vehicle rear-view?',
     '[{"id":"o1","text":"Concave"},{"id":"o2","text":"Convex"},{"id":"o3","text":"Plane"}]',
     'o2',
     'Convex mirrors give a wider field of view.'),
    ('Physics',     'Electricity', 'MEDIUM',
     'Two 6-ohm resistors are connected in parallel. What is the equivalent resistance?',
     '[{"id":"o1","text":"3 ohm"},{"id":"o2","text":"6 ohm"},{"id":"o3","text":"12 ohm"}]',
     'o1',
     '1/Req = 1/6 + 1/6 = 1/3 -> Req = 3 ohm.'),
    ('Mathematics', 'Real Numbers', 'EASY',
     'Which of the following is irrational?',
     '[{"id":"o1","text":"22/7"},{"id":"o2","text":"sqrt(2)"},{"id":"o3","text":"-3"}]',
     'o2',
     'sqrt(2) cannot be expressed as p/q with integer p, q.'),
    ('Mathematics', 'Polynomials', 'HARD',
     'Number of zeros of the polynomial p(x) = x^4 - 1?',
     '[{"id":"o1","text":"2"},{"id":"o2","text":"3"},{"id":"o3","text":"4"}]',
     'o3',
     'x^4 - 1 = (x-1)(x+1)(x^2+1) -> 2 real + 2 complex zeros = 4 in C.'),
    ('Science',     'Acids, Bases and Salts', 'MEDIUM',
     'Which gas is liberated when an acid reacts with a metal?',
     '[{"id":"o1","text":"Oxygen"},{"id":"o2","text":"Hydrogen"},{"id":"o3","text":"Carbon dioxide"}]',
     'o2',
     'Active metals displace H2 from acids.')
) AS v(subject, chapter, difficulty, text, options_json, correct, explanation)
LEFT JOIN chapter c ON c.name = v.chapter
WHERE NOT EXISTS (
    SELECT 1 FROM quiz_question q WHERE q.text = v.text
);

-- 7. previous_year_question --------------------------------------------
INSERT INTO previous_year_question (chapter_id, topic_id, year, board, difficulty, language, question_type, marks, text, model_answer)
SELECT c.id, NULL, v.year, v.board, v.difficulty::varchar, 'en', v.qtype, v.marks, v.text, v.model
FROM (VALUES
    ('Light - Reflection', 2023, 'CBSE', 'MEDIUM', 'SHORT', 3, 'State the laws of reflection.',                                          'Two laws: (1) angle of incidence = angle of reflection; (2) incident, reflected ray and normal lie in the same plane.'),
    ('Light - Reflection', 2022, 'CBSE', 'EASY',   'MCQ',   1, 'A plane mirror image is __ inverted.',                                  'Laterally.'),
    ('Electricity',        2023, 'CBSE', 'HARD',   'LONG',  5, 'Derive Ohms law and explain the experiment to verify it.',              'V proportional to I at constant temp -> V = IR. Setup: battery, ammeter, voltmeter, rheostat...'),
    ('Real Numbers',       2024, 'CBSE', 'EASY',   'SHORT', 2, 'Prove that sqrt(2) is irrational.',                                     'Assume p/q in lowest terms; derive contradiction with parity.'),
    ('Polynomials',        2023, 'CBSE', 'MEDIUM', 'SHORT', 3, 'Find zeros of p(x) = x^2 - 5x + 6.',                                    'x = 2 and x = 3.'),
    ('Acids, Bases and Salts', 2022, 'CBSE', 'MEDIUM', 'SHORT', 3, 'Differentiate between strong and weak acids with one example each.', 'Strong: HCl (fully ionises). Weak: CH3COOH (partial ionisation).')
) AS v(chapter, year, board, difficulty, qtype, marks, text, model)
JOIN chapter c ON c.name = v.chapter
WHERE NOT EXISTS (SELECT 1 FROM previous_year_question);

COMMIT;

-- Quick check
SELECT 'subject' AS t, COUNT(*) FROM subject
UNION ALL SELECT 'chapter',                COUNT(*) FROM chapter
UNION ALL SELECT 'topic',                  COUNT(*) FROM topic
UNION ALL SELECT 'content_chunk',          COUNT(*) FROM content_chunk
UNION ALL SELECT 'quiz_question',          COUNT(*) FROM quiz_question
UNION ALL SELECT 'previous_year_question', COUNT(*) FROM previous_year_question
ORDER BY 1;
