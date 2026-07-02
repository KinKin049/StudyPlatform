INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT '偶数计数 / Even Count',
       'cn-even-count',
       '给定 n 个整数，统计其中有多少个偶数。Given n integers, count how many of them are even.',
       '第一行输入整数 n。第二行输入 n 个整数。The first line contains n. The second line contains n integers.',
       '输出一个整数，表示偶数的数量。Output one integer, the number of even values.',
       JSON_ARRAY(JSON_OBJECT('input', '6\n1 2 4 7 8 9\n', 'output', '3\n')),
       'EASY',
       1000,
       262144,
       JSON_ARRAY('beginner', 'array'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'cn-even-count');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT '回文字符串 / Palindrome String',
       'cn-palindrome-string',
       '给定一个只包含小写字母的字符串，判断它正读和反读是否完全相同。Given a lowercase string, determine whether it is a palindrome.',
       '输入一行字符串 s，长度至少为 1。Input one string s.',
       '如果是回文输出 YES，否则输出 NO。Output YES for a palindrome, otherwise output NO.',
       JSON_ARRAY(JSON_OBJECT('input', 'level\n', 'output', 'YES\n')),
       'EASY',
       1000,
       262144,
       JSON_ARRAY('beginner', 'string'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'cn-palindrome-string');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT '区间求和 / Range Sum Query',
       'cn-prefix-range-sum',
       '给定一个长度为 n 的整数数组和 q 次询问，每次询问区间 [l,r] 的元素和。Given an array and q queries, answer the sum of each inclusive range.',
       '第一行输入 n 和 q。第二行输入 n 个整数。接下来 q 行每行输入 l 和 r，位置从 1 开始。The first line contains n and q. The next line contains n integers. Each query contains l and r.',
       '对每个询问输出一行区间和。Output one line for each query.',
       JSON_ARRAY(JSON_OBJECT('input', '5 3\n1 2 3 4 5\n1 3\n2 5\n4 4\n', 'output', '6\n14\n4\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('array', 'prefix'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'cn-prefix-range-sum');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT '二分查找位置 / Binary Search Position',
       'cn-binary-search-position',
       '给定一个升序数组和目标值 x，输出 x 第一次出现的位置。如果不存在，输出 -1。Given a sorted array and target x, output the first position of x, or -1 if absent.',
       '第一行输入 n 和 x。第二行输入 n 个非降序整数。The first line contains n and x. The second line contains n sorted integers.',
       '输出目标值第一次出现的 1-based 位置，或 -1。Output the first 1-based position, or -1.',
       JSON_ARRAY(JSON_OBJECT('input', '7 4\n1 2 4 4 4 8 9\n', 'output', '3\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('array', 'binary-search'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'cn-binary-search-position');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT '网格最短路 / Grid Shortest Path',
       'cn-grid-shortest-path',
       '给定一个由可通行格子和障碍格子组成的网格，求从左上角到右下角的最短步数。每次可以向上下左右移动一格。Given a grid with obstacles, find the shortest path from the top-left cell to the bottom-right cell.',
       '第一行输入 n 和 m。接下来 n 行每行 m 个字符，. 表示可通行，# 表示障碍。The first line contains n and m, followed by the grid.',
       '输出最短步数；如果不可达，输出 -1。Output the minimum steps, or -1 if unreachable.',
       JSON_ARRAY(JSON_OBJECT('input', '3 4\n....\n.##.\n....\n', 'output', '5\n')),
       'HARD',
       1500,
       262144,
       JSON_ARRAY('bfs', 'grid', 'graph'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'cn-grid-shortest-path');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT '最长上升子序列长度 / LIS Length',
       'cn-lis-length',
       '给定一个整数序列，求最长严格上升子序列的长度。Given an integer sequence, compute the length of the longest strictly increasing subsequence.',
       '第一行输入 n。第二行输入 n 个整数。The first line contains n. The second line contains n integers.',
       '输出一个整数，表示最长上升子序列长度。Output the LIS length.',
       JSON_ARRAY(JSON_OBJECT('input', '6\n3 1 2 5 4 7\n', 'output', '4\n')),
       'HARD',
       1500,
       262144,
       JSON_ARRAY('dp', 'binary-search'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'cn-lis-length');

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '6\n1 2 4 7 8 9\n', '3\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'cn-even-count' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5\n-2 -1 0 3 6\n', '3\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'cn-even-count' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, 'level\n', 'YES\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'cn-palindrome-string' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, 'algorithm\n', 'NO\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'cn-palindrome-string' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5 3\n1 2 3 4 5\n1 3\n2 5\n4 4\n', '6\n14\n4\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'cn-prefix-range-sum' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4 2\n-1 5 6 -2\n1 4\n2 3\n', '8\n11\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'cn-prefix-range-sum' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '7 4\n1 2 4 4 4 8 9\n', '3\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'cn-binary-search-position' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5 7\n1 3 5 6 9\n', '-1\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'cn-binary-search-position' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '3 4\n....\n.##.\n....\n', '5\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'cn-grid-shortest-path' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '2 2\n.#\n#.\n', '-1\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'cn-grid-shortest-path' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '6\n3 1 2 5 4 7\n', '4\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'cn-lis-length' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5\n5 4 3 2 1\n', '1\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'cn-lis-length' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);
