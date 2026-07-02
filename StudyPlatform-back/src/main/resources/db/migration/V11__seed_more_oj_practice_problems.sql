-- 扩充 OJ 练习题库：题面为本项目原创描述，覆盖基础语法、字符串、数论、图论和动态规划。

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Array Sum',
       'array-sum',
       'Given n integers, calculate their total sum.',
       'The first line contains n. The second line contains n integers.',
       'Output one integer, the sum of all numbers.',
       JSON_ARRAY(JSON_OBJECT('input', '5\n1 2 3 4 5\n', 'output', '15\n')),
       'EASY',
       1000,
       262144,
       JSON_ARRAY('array', 'prefix', 'beginner'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'array-sum');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Reverse String',
       'reverse-string',
       'Given a string without spaces, output the characters in reverse order.',
       'One line contains a non-empty string s.',
       'Output the reversed string.',
       JSON_ARRAY(JSON_OBJECT('input', 'petroleum\n', 'output', 'muelortep\n')),
       'EASY',
       1000,
       262144,
       JSON_ARRAY('string', 'beginner'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'reverse-string');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'GCD and LCM',
       'gcd-and-lcm',
       'Given two positive integers, output their greatest common divisor and least common multiple.',
       'One line contains two positive integers a and b.',
       'Output gcd(a,b) and lcm(a,b), separated by one space.',
       JSON_ARRAY(JSON_OBJECT('input', '12 18\n', 'output', '6 36\n')),
       'EASY',
       1000,
       262144,
       JSON_ARRAY('math', 'number-theory'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'gcd-and-lcm');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Count Primes',
       'count-primes',
       'Count how many prime numbers are not greater than n.',
       'One line contains integer n.',
       'Output one integer, the number of primes in [2,n].',
       JSON_ARRAY(JSON_OBJECT('input', '10\n', 'output', '4\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('math', 'sieve'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'count-primes');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Valid Brackets',
       'valid-brackets',
       'Determine whether a bracket string is valid. The string may contain (), [] and {}.',
       'One line contains a bracket string.',
       'Output YES if the string is valid, otherwise output NO.',
       JSON_ARRAY(JSON_OBJECT('input', '([]{})\n', 'output', 'YES\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('stack', 'string'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'valid-brackets');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Two Sum Indices',
       'two-sum-indices',
       'Find two different positions whose values add up to target. If multiple answers exist, output the first pair found by scanning i from left to right and j after i.',
       'The first line contains n and target. The second line contains n integers.',
       'Output two 1-based indices, or -1 if no pair exists.',
       JSON_ARRAY(JSON_OBJECT('input', '5 9\n2 7 11 15 1\n', 'output', '1 2\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('array', 'hash-table'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'two-sum-indices');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Longest Increasing Subsequence',
       'longest-increasing-subsequence',
       'Given a sequence of integers, calculate the length of its longest strictly increasing subsequence.',
       'The first line contains n. The second line contains n integers.',
       'Output one integer, the LIS length.',
       JSON_ARRAY(JSON_OBJECT('input', '6\n10 9 2 5 3 7\n', 'output', '3\n')),
       'HARD',
       1500,
       262144,
       JSON_ARRAY('dp', 'binary-search'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'longest-increasing-subsequence');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Unweighted Shortest Path',
       'unweighted-shortest-path',
       'Given an undirected unweighted graph, find the minimum number of edges from s to t.',
       'The first line contains n, m, s and t. The next m lines contain edges u v.',
       'Output the shortest distance, or -1 if t is unreachable.',
       JSON_ARRAY(JSON_OBJECT('input', '4 3 1 4\n1 2\n2 3\n3 4\n', 'output', '3\n')),
       'MEDIUM',
       1500,
       262144,
       JSON_ARRAY('graph', 'bfs'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'unweighted-shortest-path');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Merge Intervals Count',
       'merge-intervals-count',
       'Given several closed intervals, merge all overlapping intervals and output how many intervals remain.',
       'The first line contains n. Each of the next n lines contains l and r.',
       'Output one integer, the number of intervals after merging.',
       JSON_ARRAY(JSON_OBJECT('input', '4\n1 3\n2 6\n8 10\n9 12\n', 'output', '2\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('sort', 'interval'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'merge-intervals-count');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Grid Path Count',
       'grid-path-count',
       'Count paths from the top-left cell to the bottom-right cell in a grid with blocked cells. You may only move right or down.',
       'The first line contains n and m. The next n lines contain m characters, where . is passable and # is blocked.',
       'Output the number of paths modulo 1000000007.',
       JSON_ARRAY(JSON_OBJECT('input', '3 3\n...\n.#.\n...\n', 'output', '2\n')),
       'HARD',
       1500,
       262144,
       JSON_ARRAY('dp', 'grid'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'grid-path-count');

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5\n1 2 3 4 5\n', '15\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'array-sum' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4\n-5 10 0 7\n', '12\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'array-sum' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, 'petroleum\n', 'muelortep\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'reverse-string' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, 'algorithm\n', 'mhtirogla\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'reverse-string' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '12 18\n', '6 36\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'gcd-and-lcm' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '21 6\n', '3 42\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'gcd-and-lcm' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '10\n', '4\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'count-primes' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '100\n', '25\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'count-primes' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '([]{})\n', 'YES\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'valid-brackets' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '([)]\n', 'NO\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'valid-brackets' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5 9\n2 7 11 15 1\n', '1 2\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'two-sum-indices' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4 100\n1 2 3 4\n', '-1\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'two-sum-indices' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '6\n10 9 2 5 3 7\n', '3\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'longest-increasing-subsequence' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '8\n1 3 6 7 9 4 10 5\n', '6\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'longest-increasing-subsequence' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4 3 1 4\n1 2\n2 3\n3 4\n', '3\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'unweighted-shortest-path' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5 2 1 5\n1 2\n3 4\n', '-1\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'unweighted-shortest-path' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4\n1 3\n2 6\n8 10\n9 12\n', '2\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'merge-intervals-count' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '3\n1 2\n3 4\n5 6\n', '3\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'merge-intervals-count' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '3 3\n...\n.#.\n...\n', '2\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'grid-path-count' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '2 3\n...\n...\n', '3\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'grid-path-count' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);
