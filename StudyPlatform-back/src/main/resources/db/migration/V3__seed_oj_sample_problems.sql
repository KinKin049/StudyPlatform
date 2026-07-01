INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'A + B Problem',
       'a-plus-b',
       'Read two integers and output their sum.',
       'Two integers a and b, separated by whitespace.',
       'One integer, the sum of a and b.',
       JSON_ARRAY(JSON_OBJECT('input', '1 2\n', 'output', '3\n')),
       'EASY',
       1000,
       262144,
       JSON_ARRAY('math', 'beginner'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'a-plus-b');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Maximum Number',
       'maximum-number',
       'Read n integers and output the maximum value.',
       'The first line contains n. The second line contains n integers.',
       'One integer, the maximum value.',
       JSON_ARRAY(JSON_OBJECT('input', '5\n1 9 3 7 4\n', 'output', '9\n')),
       'EASY',
       1000,
       262144,
       JSON_ARRAY('array', 'beginner'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'maximum-number');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Fibonacci',
       'fibonacci',
       'Given n, output the n-th Fibonacci number. Define F(0)=0 and F(1)=1.',
       'One integer n. 0 <= n <= 45.',
       'One integer, F(n).',
       JSON_ARRAY(JSON_OBJECT('input', '10\n', 'output', '55\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('dp', 'recursion'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'fibonacci');

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '1 2\n', '3\n', 1, 1, 1
FROM oj_problems p
WHERE p.slug = 'a-plus-b'
  AND NOT EXISTS (
    SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1
  );

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '-5 11\n', '6\n', 0, 1, 2
FROM oj_problems p
WHERE p.slug = 'a-plus-b'
  AND NOT EXISTS (
    SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2
  );

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5\n1 9 3 7 4\n', '9\n', 1, 1, 1
FROM oj_problems p
WHERE p.slug = 'maximum-number'
  AND NOT EXISTS (
    SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1
  );

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4\n-8 -3 -12 -4\n', '-3\n', 0, 1, 2
FROM oj_problems p
WHERE p.slug = 'maximum-number'
  AND NOT EXISTS (
    SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2
  );

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '10\n', '55\n', 1, 1, 1
FROM oj_problems p
WHERE p.slug = 'fibonacci'
  AND NOT EXISTS (
    SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1
  );

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '45\n', '1134903170\n', 0, 1, 2
FROM oj_problems p
WHERE p.slug = 'fibonacci'
  AND NOT EXISTS (
    SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2
  );
