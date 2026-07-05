-- Expand the OJ problem bank with original bilingual practice problems.

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Maximum Subarray Sum',
       'maximum-subarray-sum',
       '给定一个整数数组，求连续子数组的最大和。Given an integer array, compute the maximum possible sum of a non-empty contiguous subarray.',
       'The first line contains n. The second line contains n integers.',
       'Output one integer, the maximum subarray sum.',
       JSON_ARRAY(JSON_OBJECT('input', '9\n-2 1 -3 4 -1 2 1 -5 4\n', 'output', '6\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('array', 'dp'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'maximum-subarray-sum');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Longest Unique Substring',
       'longest-unique-substring',
       '给定一个字符串，求不含重复字符的最长连续子串长度。Given a string, return the length of the longest substring without repeated characters.',
       'One line contains a string s.',
       'Output one integer, the answer.',
       JSON_ARRAY(JSON_OBJECT('input', 'abcabcbb\n', 'output', '3\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('string', 'hash-table'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'longest-unique-substring');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Minimum Size Subarray Sum',
       'minimum-size-subarray-sum',
       '给定正整数数组和目标值 target，求和至少为 target 的最短连续子数组长度。Given a positive integer array and a target, find the minimum length of a contiguous subarray whose sum is at least target.',
       'The first line contains n and target. The second line contains n positive integers.',
       'Output the minimum length, or 0 if no such subarray exists.',
       JSON_ARRAY(JSON_OBJECT('input', '6 7\n2 3 1 2 4 3\n', 'output', '2\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('array', 'prefix', 'binary-search'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'minimum-size-subarray-sum');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Next Greater Element Right',
       'next-greater-element-right',
       '给定数组，对每个位置输出其右侧第一个更大的元素，不存在则输出 -1。For each element in the array, output the first greater value on its right side, or -1 if it does not exist.',
       'The first line contains n. The second line contains n integers.',
       'Output n integers separated by spaces.',
       JSON_ARRAY(JSON_OBJECT('input', '4\n2 1 2 4\n', 'output', '4 2 4 -1\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('array', 'stack'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'next-greater-element-right');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Daily Temperatures Wait',
       'daily-temperatures-wait',
       '给定每日气温，输出每一天还要等待多少天才会升温。Given daily temperatures, output for each day how many days must be waited until a warmer temperature appears.',
       'The first line contains n. The second line contains n integers.',
       'Output n integers separated by spaces.',
       JSON_ARRAY(JSON_OBJECT('input', '8\n73 74 75 71 69 72 76 73\n', 'output', '1 1 4 2 1 1 0 0\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('array', 'stack'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'daily-temperatures-wait');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Sliding Window Maximum',
       'sliding-window-maximum',
       '给定数组和窗口大小 k，输出每个长度为 k 的滑动窗口中的最大值。Given an array and window size k, output the maximum value in every sliding window of length k.',
       'The first line contains n and k. The second line contains n integers.',
       'Output the maximum values in order, separated by spaces.',
       JSON_ARRAY(JSON_OBJECT('input', '8 3\n1 3 -1 -3 5 3 6 7\n', 'output', '3 3 5 5 6 7\n')),
       'HARD',
       1500,
       262144,
       JSON_ARRAY('array', 'stack'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'sliding-window-maximum');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Shortest Unsorted Subarray',
       'shortest-unsorted-subarray',
       '给定整数数组，求最短连续区间，使得只排序这段后整个数组有序。Given an integer array, find the length of the shortest continuous subarray that must be sorted so that the whole array becomes nondecreasing.',
       'The first line contains n. The second line contains n integers.',
       'Output one integer, the required length.',
       JSON_ARRAY(JSON_OBJECT('input', '7\n2 6 4 8 10 9 15\n', 'output', '5\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('array', 'sort'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'shortest-unsorted-subarray');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Search Rotated Position',
       'search-rotated-position',
       '给定一个经过旋转的严格升序数组和目标值，输出目标值的下标。Given a rotated strictly increasing array and a target value, output the zero-based index of the target, or -1 if it is absent.',
       'The first line contains n and target. The second line contains n integers.',
       'Output one integer, the zero-based index or -1.',
       JSON_ARRAY(JSON_OBJECT('input', '7 0\n4 5 6 7 0 1 2\n', 'output', '4\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('array', 'binary-search'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'search-rotated-position');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Spiral Matrix Traverse',
       'spiral-matrix-traverse',
       '给定一个矩阵，按顺时针螺旋顺序输出所有元素。Given a matrix, print all elements in clockwise spiral order.',
       'The first line contains n and m. The next n lines each contain m integers.',
       'Output all visited values separated by spaces.',
       JSON_ARRAY(JSON_OBJECT('input', '3 3\n1 2 3\n4 5 6\n7 8 9\n', 'output', '1 2 3 6 9 8 7 4 5\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('array'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'spiral-matrix-traverse');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Count Islands Grid',
       'count-islands-grid',
       '给定由 0 和 1 组成的网格，统计上下左右相连的岛屿数量。Given a grid of 0 and 1, count the number of islands connected by four directions.',
       'The first line contains n and m. The next n lines each contain a string of length m using only 0 and 1.',
       'Output one integer, the number of islands.',
       JSON_ARRAY(JSON_OBJECT('input', '4 5\n11000\n11000\n00100\n00011\n', 'output', '3\n')),
       'MEDIUM',
       1500,
       262144,
       JSON_ARRAY('graph', 'bfs', 'grid'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'count-islands-grid');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Flood Fill Area',
       'flood-fill-area',
       '给定网格和起点，统计与起点同字符且四向连通的区域大小。Given a grid and a starting cell, count the size of the connected component that has the same character as the start cell.',
       'The first line contains n and m. The next n lines are the grid. The last line contains r and c using zero-based indices.',
       'Output one integer, the component size.',
       JSON_ARRAY(JSON_OBJECT('input', '3 4\nAABA\nAACA\nBBBB\n0 0\n', 'output', '4\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('bfs', 'grid'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'flood-fill-area');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Can Finish Tasks',
       'can-finish-tasks',
       '给定若干任务及其依赖关系，判断是否可以完成全部任务。Given task dependency pairs, determine whether all tasks can be finished.',
       'The first line contains n and m. Each of the next m lines contains a pair a b meaning task a depends on task b.',
       'Output YES if all tasks can be completed, otherwise output NO.',
       JSON_ARRAY(JSON_OBJECT('input', '4 3\n1 0\n2 1\n3 2\n', 'output', 'YES\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('graph', 'bfs'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'can-finish-tasks');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Weighted Shortest Path',
       'weighted-shortest-path',
       '给定带正权有向图，求从 1 号点到 n 号点的最短路。Given a directed graph with positive edge weights, find the shortest path distance from node 1 to node n.',
       'The first line contains n and m. Each of the next m lines contains u v w.',
       'Output the shortest distance, or -1 if node n is unreachable.',
       JSON_ARRAY(JSON_OBJECT('input', '4 5\n1 2 2\n1 3 5\n2 3 1\n2 4 4\n3 4 1\n', 'output', '4\n')),
       'HARD',
       1500,
       262144,
       JSON_ARRAY('graph'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'weighted-shortest-path');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Count Connected Components',
       'count-connected-components',
       '给定无向图，统计连通块数量。Given an undirected graph, count how many connected components it has.',
       'The first line contains n and m. Each of the next m lines contains an edge u v.',
       'Output one integer, the number of connected components.',
       JSON_ARRAY(JSON_OBJECT('input', '5 2\n1 2\n4 5\n', 'output', '3\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('graph', 'bfs'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'count-connected-components');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Knapsack 01 Value',
       'knapsack-01-value',
       '给定若干物品的重量和价值以及背包容量，求能获得的最大总价值。Given item weights and values and a knapsack capacity, compute the maximum value obtainable when each item can be chosen at most once.',
       'The first line contains n and capacity. Each of the next n lines contains weight and value.',
       'Output one integer, the maximum total value.',
       JSON_ARRAY(JSON_OBJECT('input', '4 5\n1 2\n2 4\n3 4\n4 5\n', 'output', '8\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('dp'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'knapsack-01-value');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Coin Change Minimum',
       'coin-change-minimum',
       '给定若干硬币面值和目标金额，求凑出目标金额所需的最少硬币数。Given coin denominations and a target amount, return the minimum number of coins needed to reach the target.',
       'The first line contains n and amount. The second line contains n coin values.',
       'Output the minimum count, or -1 if it is impossible.',
       JSON_ARRAY(JSON_OBJECT('input', '3 11\n1 2 5\n', 'output', '3\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('dp'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'coin-change-minimum');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Edit Distance Steps',
       'edit-distance-steps',
       '给定两个字符串，求把第一个字符串变成第二个字符串所需的最少编辑次数。Given two strings, compute the minimum number of insert, delete, or replace operations required to transform the first string into the second.',
       'The input contains two lines, the two strings.',
       'Output one integer, the edit distance.',
       JSON_ARRAY(JSON_OBJECT('input', 'horse\nros\n', 'output', '3\n')),
       'HARD',
       1500,
       262144,
       JSON_ARRAY('dp', 'string'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'edit-distance-steps');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Longest Common Subsequence Length',
       'longest-common-subsequence-length',
       '给定两个字符串，求最长公共子序列长度。Given two strings, compute the length of their longest common subsequence.',
       'The input contains two lines, the two strings.',
       'Output one integer, the LCS length.',
       JSON_ARRAY(JSON_OBJECT('input', 'abcde\nace\n', 'output', '3\n')),
       'MEDIUM',
       1500,
       262144,
       JSON_ARRAY('dp', 'string'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'longest-common-subsequence-length');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Partition Equal Subset',
       'partition-equal-subset',
       '给定正整数数组，判断是否可以分成两个元素和相等的子集。Given a positive integer array, determine whether it can be partitioned into two subsets with equal sum.',
       'The first line contains n. The second line contains n positive integers.',
       'Output YES if such a partition exists, otherwise output NO.',
       JSON_ARRAY(JSON_OBJECT('input', '4\n1 5 11 5\n', 'output', 'YES\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('dp', 'array'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'partition-equal-subset');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Perfect Squares Min Count',
       'perfect-squares-min-count',
       '给定整数 n，求和为 n 的完全平方数最少需要多少个。Given an integer n, find the minimum number of perfect squares whose sum equals n.',
       'One line contains integer n.',
       'Output one integer, the minimum count.',
       JSON_ARRAY(JSON_OBJECT('input', '12\n', 'output', '3\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('dp', 'math'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'perfect-squares-min-count');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Minimum Path Sum Grid',
       'minimum-path-sum-grid',
       '给定非负整数网格，只能向右或向下移动，求从左上到右下的最小路径和。Given a nonnegative grid, moving only right or down, compute the minimum path sum from the top-left cell to the bottom-right cell.',
       'The first line contains n and m. The next n lines each contain m nonnegative integers.',
       'Output one integer, the minimum path sum.',
       JSON_ARRAY(JSON_OBJECT('input', '3 3\n1 3 1\n1 5 1\n4 2 1\n', 'output', '7\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('dp', 'grid'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'minimum-path-sum-grid');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Triangle Minimum Path',
       'triangle-minimum-path',
       '给定数字三角形，每步只能走到下一层相邻位置，求最小路径和。Given a number triangle, moving to adjacent positions in the next row, compute the minimum path sum from top to bottom.',
       'The first line contains n. The next n lines contain 1 to n integers respectively.',
       'Output one integer, the minimum path sum.',
       JSON_ARRAY(JSON_OBJECT('input', '4\n2\n3 4\n6 5 7\n4 1 8 3\n', 'output', '11\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('dp'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'triangle-minimum-path');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Basic Expression Evaluation',
       'basic-expression-evaluation',
       '给定只包含非负整数、加减号和圆括号的表达式，求其值。Given an arithmetic expression containing nonnegative integers, plus, minus, and parentheses, evaluate its value.',
       'One line contains the expression without spaces.',
       'Output one integer, the evaluated result.',
       JSON_ARRAY(JSON_OBJECT('input', '1+(2-(3-4))\n', 'output', '4\n')),
       'HARD',
       1500,
       262144,
       JSON_ARRAY('stack', 'string'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'basic-expression-evaluation');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Sliding Window Minimum',
       'sliding-window-minimum',
       '给定数组和窗口大小 k，输出每个长度为 k 的滑动窗口中的最小值。Given an array and window size k, output the minimum value in every sliding window of length k.',
       'The first line contains n and k. The second line contains n integers.',
       'Output the minimum values in order, separated by spaces.',
       JSON_ARRAY(JSON_OBJECT('input', '8 3\n1 3 -1 -3 5 3 6 7\n', 'output', '-1 -3 -3 -3 3 3\n')),
       'HARD',
       1500,
       262144,
       JSON_ARRAY('array', 'stack'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'sliding-window-minimum');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Range Add Final Array',
       'range-add-final-array',
       '给定长度为 n 的初始全零数组和若干区间加操作，输出所有操作完成后的数组。Given an all-zero array of length n and several range increment operations, output the final array after all operations.',
       'The first line contains n and q. Each of the next q lines contains l r val using 1-based inclusive indices.',
       'Output the final array separated by spaces.',
       JSON_ARRAY(JSON_OBJECT('input', '5 3\n1 3 2\n2 5 1\n4 4 3\n', 'output', '2 3 3 4 1\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('array', 'prefix'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'range-add-final-array');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Prefix XOR Query',
       'prefix-xor-query',
       '给定整数数组和若干查询，输出每个区间的异或值。Given an integer array and several queries, output the XOR value of each inclusive range.',
       'The first line contains n and q. The second line contains n integers. Each of the next q lines contains l and r using 1-based indices.',
       'Output one line per query.',
       JSON_ARRAY(JSON_OBJECT('input', '4 3\n5 1 7 3\n1 2\n2 4\n3 3\n', 'output', '4\n5\n7\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('array', 'prefix'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'prefix-xor-query');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Palindrome Substring Count',
       'palindrome-substring-count',
       '给定字符串，统计其中回文子串的总数。Given a string, count how many substrings are palindromes.',
       'One line contains a string s.',
       'Output one integer, the count of palindromic substrings.',
       JSON_ARRAY(JSON_OBJECT('input', 'aaa\n', 'output', '6\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('string', 'dp'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'palindrome-substring-count');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Validate Stack Sequences',
       'validate-stack-sequences',
       '给定入栈序列和出栈序列，判断是否可能来自同一个栈。Given push and pop sequences, determine whether they can come from the same stack.',
       'The first line contains n. The second line contains the push sequence. The third line contains the pop sequence.',
       'Output YES if the pop sequence is valid, otherwise output NO.',
       JSON_ARRAY(JSON_OBJECT('input', '5\n1 2 3 4 5\n4 5 3 2 1\n', 'output', 'YES\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('stack', 'array'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'validate-stack-sequences');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Merged Interval Total Length',
       'merged-interval-total-length',
       '给定若干闭区间，合并所有重叠部分后输出总长度，长度按 r - l 计算。Given several closed intervals, merge all overlaps and output the total covered length measured as r - l.',
       'The first line contains n. Each of the next n lines contains l and r.',
       'Output one integer, the total merged length.',
       JSON_ARRAY(JSON_OBJECT('input', '3\n1 4\n2 6\n8 10\n', 'output', '7\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('sort', 'interval'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'merged-interval-total-length');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Majority Element',
       'majority-element',
       '给定一个数组，保证存在出现次数严格大于一半的元素，输出该元素。Given an array in which a majority element is guaranteed to exist, output that element.',
       'The first line contains n. The second line contains n integers.',
       'Output one integer, the majority element.',
       JSON_ARRAY(JSON_OBJECT('input', '7\n2 2 1 1 1 2 2\n', 'output', '2\n')),
       'EASY',
       1000,
       262144,
       JSON_ARRAY('array', 'hash-table'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'majority-element');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Subarray Sum Equals K',
       'subarray-sum-equals-k',
       '给定整数数组和整数 k，统计和恰好等于 k 的连续子数组个数。Given an integer array and an integer k, count the number of contiguous subarrays whose sum equals k.',
       'The first line contains n and k. The second line contains n integers.',
       'Output one integer, the count.',
       JSON_ARRAY(JSON_OBJECT('input', '3 2\n1 1 1\n', 'output', '2\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('array', 'prefix', 'hash-table'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'subarray-sum-equals-k');

INSERT INTO oj_problems
(title, slug, description, input_description, output_description, samples, difficulty,
 time_limit_ms, memory_limit_kb, tags, status, created_by)
SELECT 'Product Except Self',
       'product-except-self',
       '给定数组，输出每个位置除自身之外所有元素的乘积，保证结果在 64 位有符号整数范围内。Given an array, output for each position the product of all other elements, assuming the result fits in signed 64-bit integers.',
       'The first line contains n. The second line contains n integers.',
       'Output n integers separated by spaces.',
       JSON_ARRAY(JSON_OBJECT('input', '4\n1 2 3 4\n', 'output', '24 12 8 6\n')),
       'MEDIUM',
       1000,
       262144,
       JSON_ARRAY('array', 'prefix'),
       'PUBLISHED',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM oj_problems WHERE slug = 'product-except-self');

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '9\n-2 1 -3 4 -1 2 1 -5 4\n', '6\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'maximum-subarray-sum' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5\n-5 -2 -7 -1 -8\n', '-1\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'maximum-subarray-sum' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, 'abcabcbb\n', '3\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'longest-unique-substring' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, 'bbbbb\n', '1\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'longest-unique-substring' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '6 7\n2 3 1 2 4 3\n', '2\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'minimum-size-subarray-sum' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5 20\n1 2 3 4 5\n', '0\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'minimum-size-subarray-sum' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4\n2 1 2 4\n', '4 2 4 -1\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'next-greater-element-right' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5\n5 4 3 2 1\n', '-1 -1 -1 -1 -1\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'next-greater-element-right' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '8\n73 74 75 71 69 72 76 73\n', '1 1 4 2 1 1 0 0\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'daily-temperatures-wait' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4\n80 79 78 77\n', '0 0 0 0\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'daily-temperatures-wait' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '8 3\n1 3 -1 -3 5 3 6 7\n', '3 3 5 5 6 7\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'sliding-window-maximum' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5 2\n9 7 5 3 1\n', '9 7 5 3\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'sliding-window-maximum' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '7\n2 6 4 8 10 9 15\n', '5\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'shortest-unsorted-subarray' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4\n1 2 3 4\n', '0\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'shortest-unsorted-subarray' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '7 0\n4 5 6 7 0 1 2\n', '4\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'search-rotated-position' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '7 3\n4 5 6 7 0 1 2\n', '-1\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'search-rotated-position' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '3 3\n1 2 3\n4 5 6\n7 8 9\n', '1 2 3 6 9 8 7 4 5\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'spiral-matrix-traverse' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '2 4\n1 2 3 4\n5 6 7 8\n', '1 2 3 4 8 7 6 5\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'spiral-matrix-traverse' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4 5\n11000\n11000\n00100\n00011\n', '3\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'count-islands-grid' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '3 3\n000\n000\n000\n', '0\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'count-islands-grid' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '3 4\nAABA\nAACA\nBBBB\n0 0\n', '4\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'flood-fill-area' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '3 3\nABA\nBAB\nABA\n1 1\n', '1\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'flood-fill-area' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4 3\n1 0\n2 1\n3 2\n', 'YES\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'can-finish-tasks' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '2 2\n0 1\n1 0\n', 'NO\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'can-finish-tasks' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4 5\n1 2 2\n1 3 5\n2 3 1\n2 4 4\n3 4 1\n', '4\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'weighted-shortest-path' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '3 1\n1 2 5\n', '-1\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'weighted-shortest-path' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5 2\n1 2\n4 5\n', '3\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'count-connected-components' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4 3\n1 2\n2 3\n3 4\n', '1\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'count-connected-components' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4 5\n1 2\n2 4\n3 4\n4 5\n', '8\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'knapsack-01-value' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '3 3\n2 3\n3 4\n4 5\n', '4\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'knapsack-01-value' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '3 11\n1 2 5\n', '3\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'coin-change-minimum' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '2 3\n2 4\n', '-1\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'coin-change-minimum' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, 'horse\nros\n', '3\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'edit-distance-steps' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, 'intention\nexecution\n', '5\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'edit-distance-steps' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, 'abcde\nace\n', '3\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'longest-common-subsequence-length' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, 'abc\ndef\n', '0\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'longest-common-subsequence-length' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4\n1 5 11 5\n', 'YES\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'partition-equal-subset' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4\n1 2 3 5\n', 'NO\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'partition-equal-subset' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '12\n', '3\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'perfect-squares-min-count' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '13\n', '2\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'perfect-squares-min-count' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '3 3\n1 3 1\n1 5 1\n4 2 1\n', '7\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'minimum-path-sum-grid' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '2 2\n1 2\n1 1\n', '3\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'minimum-path-sum-grid' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4\n2\n3 4\n6 5 7\n4 1 8 3\n', '11\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'triangle-minimum-path' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '3\n-1\n2 3\n1 -1 -3\n', '-2\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'triangle-minimum-path' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '1+(2-(3-4))\n', '4\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'basic-expression-evaluation' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '10-(2+3)-(4-1)\n', '2\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'basic-expression-evaluation' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '8 3\n1 3 -1 -3 5 3 6 7\n', '-1 -3 -3 -3 3 3\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'sliding-window-minimum' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5 2\n9 7 5 3 1\n', '7 5 3 1\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'sliding-window-minimum' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5 3\n1 3 2\n2 5 1\n4 4 3\n', '2 3 3 4 1\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'range-add-final-array' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4 2\n1 4 5\n2 3 -2\n', '5 3 3 5\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'range-add-final-array' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4 3\n5 1 7 3\n1 2\n2 4\n3 3\n', '4\n5\n7\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'prefix-xor-query' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5 2\n1 2 3 4 5\n1 5\n4 5\n', '1\n1\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'prefix-xor-query' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, 'aaa\n', '6\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'palindrome-substring-count' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, 'abc\n', '3\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'palindrome-substring-count' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5\n1 2 3 4 5\n4 5 3 2 1\n', 'YES\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'validate-stack-sequences' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5\n1 2 3 4 5\n4 3 5 1 2\n', 'NO\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'validate-stack-sequences' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '3\n1 4\n2 6\n8 10\n', '7\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'merged-interval-total-length' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '2\n0 1\n2 5\n', '4\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'merged-interval-total-length' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '7\n2 2 1 1 1 2 2\n', '2\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'majority-element' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5\n3 3 4 2 3\n', '3\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'majority-element' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '3 2\n1 1 1\n', '2\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'subarray-sum-equals-k' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5 3\n1 2 1 2 1\n', '4\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'subarray-sum-equals-k' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4\n1 2 3 4\n', '24 12 8 6\n', 1, 1, 1 FROM oj_problems p
WHERE p.slug = 'product-except-self' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 1);
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5\n-1 1 0 -3 3\n', '0 0 9 0 0\n', 0, 1, 2 FROM oj_problems p
WHERE p.slug = 'product-except-self' AND NOT EXISTS (SELECT 1 FROM oj_test_cases tc WHERE tc.problem_id = p.id AND tc.sort_order = 2);
