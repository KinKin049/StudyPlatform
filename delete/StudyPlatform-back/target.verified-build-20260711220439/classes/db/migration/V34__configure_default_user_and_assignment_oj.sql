INSERT IGNORE INTO users (id, username, password_hash, nickname, role, enabled)
VALUES (1, 'local_default_student_1', 'local-default-password', '默认学生', 'STUDENT', 1);

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Max Of Three',
       'assignment-max-of-three',
       'Implement function maxOfThree(int a, int b, int c), and return the maximum value among the three integers.',
       'Three integers a, b and c, separated by whitespace.',
       'One integer, the maximum value.',
       JSON_ARRAY(JSON_OBJECT('input', '1 2 3\n', 'output', '3\n')),
       'EASY',
       1000,
       262144,
       JSON_ARRAY('function', 'beginner', 'assignment'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'assignment-max-of-three');

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '1 2 3\n', '3\n', 1, 1, 1
FROM oj_problems p
WHERE p.slug = 'assignment-max-of-three'
  AND NOT EXISTS (
    SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1
  );

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '-8 -3 -12\n', '-3\n', 0, 1, 2
FROM oj_problems p
WHERE p.slug = 'assignment-max-of-three'
  AND NOT EXISTS (
    SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2
  );

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '42 42 7\n', '42\n', 0, 1, 3
FROM oj_problems p
WHERE p.slug = 'assignment-max-of-three'
  AND NOT EXISTS (
    SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 3
  );

UPDATE academy_assignment_questions q
JOIN academy_assignments a ON a.id = q.assignment_id
JOIN oj_problems p ON p.slug = 'assignment-max-of-three'
SET q.oj_problem_id = p.id,
    q.answer_explanation = '编程题已关联 OJ 判题，提交后会自动运行测试用例，并进入教师审核。'
WHERE a.assignment_code = 'c-function-practice'
  AND q.question_type = 'code'
  AND q.question_title LIKE '%maxOfThree%';
