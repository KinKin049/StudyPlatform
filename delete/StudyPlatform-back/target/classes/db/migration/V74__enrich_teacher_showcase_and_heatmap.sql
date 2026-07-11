-- 增强第七部分功能展示数据：热力图强弱分布、教师信箱、教师待办、教师 OJ 题库。

DELETE FROM profile_learning_time_records
WHERE user_id = 9001 AND target_code LIKE 'demo-heatmap-v69:%';

INSERT INTO profile_learning_time_records
  (user_id, module_type, target_code, target_title, duration_seconds, created_at)
SELECT
  9001,
  CASE MOD(days.n, 6)
    WHEN 0 THEN 'video'
    WHEN 1 THEN 'question_bank'
    WHEN 2 THEN 'oj'
    WHEN 3 THEN 'visualization'
    WHEN 4 THEN 'assignment'
    ELSE 'petroleum'
  END,
  CONCAT('demo-heatmap-v69:', days.n, ':', slots.s),
  CASE MOD(days.n, 6)
    WHEN 0 THEN '在线课堂章节学习'
    WHEN 1 THEN '题库专项练习'
    WHEN 2 THEN 'OJ代码训练'
    WHEN 3 THEN '算法可视化复盘'
    WHEN 4 THEN '课程作业推进'
    ELSE '油气仿真实验'
  END,
  600 + MOD(days.n * 97 + slots.s * 137, 2400),
  DATE_ADD(DATE_SUB(CURRENT_DATE, INTERVAL days.n DAY), INTERVAL (7 + MOD(slots.s, 12)) HOUR)
FROM (
  SELECT ones.i + tens.i * 10 + hundreds.i * 100 AS n
  FROM (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
  CROSS JOIN (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
  CROSS JOIN (SELECT 0 i UNION ALL SELECT 1) hundreds
  WHERE ones.i + tens.i * 10 + hundreds.i * 100 < 119
) days
CROSS JOIN (
  SELECT 0 s UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
  UNION ALL SELECT 10 UNION ALL SELECT 11
) slots
WHERE slots.s < CASE MOD(days.n, 10)
  WHEN 0 THEN 1
  WHEN 1 THEN 2
  WHEN 2 THEN 3
  WHEN 3 THEN 4
  WHEN 4 THEN 6
  WHEN 5 THEN 8
  WHEN 6 THEN 10
  WHEN 7 THEN 12
  WHEN 8 THEN 5
  ELSE 1
END;

DELETE FROM academy_course_reviews
WHERE resource_type = 'online-open-courses'
  AND course_id = 'teacher-demo-data-structure-2026'
  AND user_name IN ('林雨桐', '马知远', '许清荷', '段亦航', '宋嘉宁', '赵景铄', '报告展示学生');

INSERT INTO academy_course_reviews
  (resource_type, course_id, user_id, user_name, rating, content, reply_content, reply_user_id,
   reply_user_name, reply_user_role_type, replied_at, teacher_read_at, created_at)
VALUES
  ('online-open-courses', 'teacher-demo-data-structure-2026', 9001, '林雨桐', 5,
   '老师，图的最短路径这部分能不能再补一个 Dijkstra 和 BFS 对照的例题？我做 OJ 时容易把边权条件混在一起。',
   NULL, NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
  ('online-open-courses', 'teacher-demo-data-structure-2026', NULL, '马知远', 4,
   '树的遍历动画很清楚，希望作业里能增加一道递归转非递归的题目，便于理解栈的作用。',
   NULL, NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 7 HOUR)),
  ('online-open-courses', 'teacher-demo-data-structure-2026', NULL, '许清荷', 5,
   'OJ 题目的样例解释很有帮助，能不能在课程详情里标注每周建议完成的题量？',
   NULL, NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
  ('online-open-courses', 'teacher-demo-data-structure-2026', NULL, '段亦航', 5,
   '排序算法部分希望增加稳定排序和非稳定排序的对照表，复习时会更直观。',
   NULL, NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
  ('online-open-courses', 'teacher-demo-data-structure-2026', NULL, '宋嘉宁', 4,
   '链表章节跟着动画做完之后再写代码顺很多，建议把课后作业截止时间放宽到周日晚。',
   '收到，后续作业会统一设置到周日晚 23:59。', 9002, '陈思远', 'teacher',
   DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
  ('online-open-courses', 'teacher-demo-data-structure-2026', NULL, '赵景铄', 5,
   '课程视频和可视化页可以配合起来看，比较适合考试前快速回顾。',
   '谢谢建议，我会把可视化链接整理到每一章的课程说明中。', 9002, '陈思远', 'teacher',
   DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY));

INSERT IGNORE INTO oj_categories (name, sort_order)
VALUES ('数组与前缀和', 20), ('链表与栈队列', 21), ('树与递归', 22), ('图论基础', 23), ('排序与查找', 24);

DELETE FROM oj_test_cases
WHERE problem_id IN (
  SELECT id FROM oj_problems
  WHERE slug IN ('teacher-prefix-oil-log', 'teacher-stack-brackets', 'teacher-tree-depth',
                 'teacher-graph-components', 'teacher-sort-stability')
);
DELETE FROM oj_problems
WHERE slug IN ('teacher-prefix-oil-log', 'teacher-stack-brackets', 'teacher-tree-depth',
               'teacher-graph-components', 'teacher-sort-stability');

INSERT INTO oj_problems
  (title, slug, category, description, input_description, output_description, standard_code,
   samples, difficulty, time_limit_ms, memory_limit_kb, tags, status, created_by, created_at)
VALUES
  ('测井曲线区间和', 'teacher-prefix-oil-log', '数组与前缀和',
   '给定 n 个测井采样值和 q 个区间，输出每个区间的采样值总和，用于训练前缀和思想。',
   '第一行输入 n 和 q；第二行输入 n 个整数；接下来 q 行每行输入 l r。',
   '输出 q 行，每行一个区间和。',
   '#include <bits/stdc++.h>\nusing namespace std;\nint main(){ios::sync_with_stdio(false);cin.tie(nullptr);int n,q;if(!(cin>>n>>q)) return 0;vector<long long>s(n+1);for(int i=1;i<=n;i++){long long x;cin>>x;s[i]=s[i-1]+x;}while(q--){int l,r;cin>>l>>r;cout<<s[r]-s[l-1]<<"\\n";}return 0;}',
   NULL, 'EASY', 1000, 262144, JSON_ARRAY('数组与前缀和','EASY','CPP'), 'PUBLISHED', 9002, DATE_SUB(NOW(), INTERVAL 11 DAY)),
  ('括号序列合法性检查', 'teacher-stack-brackets', '链表与栈队列',
   '判断只包含三类括号的字符串是否合法，考查栈的基本使用。',
   '输入一行括号字符串，长度不超过 100000。',
   '合法输出 YES，否则输出 NO。',
   '#include <bits/stdc++.h>\nusing namespace std;\nint main(){string s;cin>>s;stack<char> st;map<char,char> mp={{'')'',''(''},{'']'',''[''},{''}'',''{''}};for(char c:s){if(c==''(''||c==''[''||c==''{'') st.push(c);else{if(st.empty()||st.top()!=mp[c]){cout<<"NO\\n";return 0;}st.pop();}}cout<<(st.empty()?"YES":"NO")<<"\\n";}',
   NULL, 'EASY', 1000, 262144, JSON_ARRAY('链表与栈队列','EASY','CPP'), 'PUBLISHED', 9002, DATE_SUB(NOW(), INTERVAL 10 DAY)),
  ('二叉树最大深度', 'teacher-tree-depth', '树与递归',
   '给出二叉树的父子关系，求从根节点到最深叶子节点的节点数。',
   '第一行 n；接下来 n 行给出每个节点的左孩子和右孩子编号，0 表示空。',
   '输出一个整数表示最大深度。',
   '#include <bits/stdc++.h>\nusing namespace std;\nint main(){int n;if(!(cin>>n)) return 0;vector<int> l(n+1),r(n+1),vis(n+1);for(int i=1;i<=n;i++){cin>>l[i]>>r[i];if(l[i])vis[l[i]]=1;if(r[i])vis[r[i]]=1;}int root=1;for(int i=1;i<=n;i++) if(!vis[i]) root=i;function<int(int)> dfs=[&](int u){if(!u)return 0;return max(dfs(l[u]),dfs(r[u]))+1;};cout<<dfs(root)<<"\\n";}',
   NULL, 'MEDIUM', 1500, 262144, JSON_ARRAY('树与递归','MEDIUM','CPP'), 'PUBLISHED', 9002, DATE_SUB(NOW(), INTERVAL 8 DAY)),
  ('无向图连通块计数', 'teacher-graph-components', '图论基础',
   '统计无向图中连通块的数量，适合作为 DFS/BFS 入门训练。',
   '第一行 n m；接下来 m 行输入一条无向边 u v。',
   '输出连通块数量。',
   '#include <bits/stdc++.h>\nusing namespace std;\nint main(){int n,m;cin>>n>>m;vector<vector<int>> g(n+1);while(m--){int u,v;cin>>u>>v;g[u].push_back(v);g[v].push_back(u);}vector<int> vis(n+1);int ans=0;for(int i=1;i<=n;i++)if(!vis[i]){ans++;queue<int>q;q.push(i);vis[i]=1;while(!q.empty()){int u=q.front();q.pop();for(int v:g[u])if(!vis[v])vis[v]=1,q.push(v);}}cout<<ans<<"\\n";}',
   NULL, 'MEDIUM', 1500, 262144, JSON_ARRAY('图论基础','MEDIUM','CPP'), 'PUBLISHED', 9002, DATE_SUB(NOW(), INTERVAL 6 DAY)),
  ('成绩排序与稳定输出', 'teacher-sort-stability', '排序与查找',
   '按照成绩降序、录入顺序稳定输出学生姓名，训练稳定排序和比较器设计。',
   '第一行 n；接下来 n 行输入姓名和成绩。',
   '按要求输出 n 行姓名。',
   '#include <bits/stdc++.h>\nusing namespace std;\nint main(){int n;cin>>n;vector<pair<string,int>> a(n);for(auto &x:a)cin>>x.first>>x.second;stable_sort(a.begin(),a.end(),[](auto &x,auto &y){return x.second>y.second;});for(auto &x:a)cout<<x.first<<"\\n";}',
   NULL, 'HARD', 2000, 262144, JSON_ARRAY('排序与查找','HARD','CPP'), 'PUBLISHED', 9002, DATE_SUB(NOW(), INTERVAL 4 DAY));

INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5 3\n1 3 5 7 9\n1 3\n2 5\n4 4\n', '9\n24\n7\n', 1, 1, 1
FROM oj_problems p WHERE p.slug = 'teacher-prefix-oil-log';
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '()[]{}\n', 'YES\n', 1, 1, 1 FROM oj_problems p WHERE p.slug = 'teacher-stack-brackets';
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '([)]\n', 'NO\n', 0, 1, 2 FROM oj_problems p WHERE p.slug = 'teacher-stack-brackets';
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '5\n2 3\n4 0\n0 5\n0 0\n0 0\n', '3\n', 1, 1, 1 FROM oj_problems p WHERE p.slug = 'teacher-tree-depth';
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '6 3\n1 2\n2 3\n5 6\n', '3\n', 1, 1, 1 FROM oj_problems p WHERE p.slug = 'teacher-graph-components';
INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
SELECT p.id, '4\nAlice 90\nBob 95\nCindy 90\nDavid 95\n', 'Bob\nDavid\nAlice\nCindy\n', 1, 1, 1 FROM oj_problems p WHERE p.slug = 'teacher-sort-stability';

DELETE FROM academy_assignment_submissions
WHERE assignment_id IN (
  SELECT id FROM academy_assignments
  WHERE assignment_code IN ('teacher-demo-mixed-2026', 'teacher-demo-oj-review-2026')
);
DELETE FROM academy_assignments
WHERE assignment_code IN ('teacher-demo-mixed-2026', 'teacher-demo-oj-review-2026');

INSERT INTO academy_assignments
  (assignment_code, course_resource_type, course_id, course_title, assignment_title, teacher_name,
   assignment_status, deadline_at, attempts_limit, duration_minutes, total_score, assignment_description)
VALUES
  ('teacher-demo-mixed-2026', 'online-open-courses', 'teacher-demo-data-structure-2026', '数据结构可视化与算法实践',
   '线性结构与树基础综合作业', '陈思远', '正在进行', DATE_ADD(NOW(), INTERVAL 6 DAY), 2, 60, 100,
   '覆盖单选、多选、填空和简答题，用于展示教师布置多题型作业与待批改流程。'),
  ('teacher-demo-oj-review-2026', 'online-open-courses', 'teacher-demo-data-structure-2026', '数据结构可视化与算法实践',
   'OJ编程题专项作业', '陈思远', '正在进行', DATE_ADD(NOW(), INTERVAL 9 DAY), 3, 90, 100,
   '要求学生完成前缀和与栈相关 OJ 题，并提交关键思路说明。');

SET @mixed_assignment_id := (SELECT id FROM academy_assignments WHERE assignment_code = 'teacher-demo-mixed-2026' LIMIT 1);
SET @oj_assignment_id := (SELECT id FROM academy_assignments WHERE assignment_code = 'teacher-demo-oj-review-2026' LIMIT 1);

INSERT INTO academy_assignment_questions
  (assignment_id, question_order, question_type, question_label, question_title, question_options,
   placeholder_text, score, correct_answer, answer_explanation, auto_gradable, oj_problem_id, requires_teacher_review)
VALUES
  (@mixed_assignment_id, 1, 'single', '第 1 题', '栈最适合用于处理下列哪类问题？',
   JSON_ARRAY('先进先出队列调度', '括号匹配与递归调用模拟', '无序集合查重', '磁盘页缓存置换'), NULL, 15,
   JSON_QUOTE('括号匹配与递归调用模拟'), '栈具有后进先出特征，常用于括号匹配和递归过程模拟。', 1, NULL, 0),
  (@mixed_assignment_id, 2, 'multiple', '第 2 题', '关于二叉树遍历，下列说法正确的是？',
   JSON_ARRAY('前序遍历先访问根节点', '中序遍历一定得到升序序列', '后序遍历最后访问根节点', '层序遍历通常借助队列实现'), NULL, 25,
   JSON_ARRAY('前序遍历先访问根节点', '后序遍历最后访问根节点', '层序遍历通常借助队列实现'), '只有二叉搜索树的中序遍历通常才呈升序。', 1, NULL, 0),
  (@mixed_assignment_id, 3, 'blank', '第 3 题', '队列的基本访问规则可以概括为____。',
   JSON_ARRAY(), NULL, 20, JSON_QUOTE('先进先出'), '队列是 FIFO 结构。', 1, NULL, 0),
  (@mixed_assignment_id, 4, 'short', '第 4 题', '请结合课程可视化页面说明递归遍历二叉树时调用栈的变化过程。',
   JSON_ARRAY(), '从入栈、递归返回和访问顺序三个角度回答。', 40, NULL, '重点关注调用栈变化和节点访问顺序。', 0, NULL, 1);

INSERT INTO academy_assignment_questions
  (assignment_id, question_order, question_type, question_label, question_title, question_options,
   placeholder_text, score, correct_answer, answer_explanation, auto_gradable, oj_problem_id, requires_teacher_review)
SELECT @oj_assignment_id, 1, 'code', '第 1 题', '完成“测井曲线区间和”OJ题，并提交代码。',
       JSON_ARRAY(), '请提交 C++ 代码，并说明前缀和数组的含义。', 50, NULL, 'OJ 自动判题后仍需教师查看代码风格。', 0, p.id, 1
FROM oj_problems p WHERE p.slug = 'teacher-prefix-oil-log';
INSERT INTO academy_assignment_questions
  (assignment_id, question_order, question_type, question_label, question_title, question_options,
   placeholder_text, score, correct_answer, answer_explanation, auto_gradable, oj_problem_id, requires_teacher_review)
SELECT @oj_assignment_id, 2, 'code', '第 2 题', '完成“括号序列合法性检查”OJ题，并提交代码。',
       JSON_ARRAY(), '请提交 C++ 代码，并说明栈为空时的边界处理。', 50, NULL, 'OJ 自动判题后仍需教师查看边界条件。', 0, p.id, 1
FROM oj_problems p WHERE p.slug = 'teacher-stack-brackets';

INSERT INTO academy_assignment_submissions
  (assignment_id, user_id, submission_status, answer_payload, score, teacher_feedback, submitted_at, created_at)
VALUES
  (@mixed_assignment_id, 9001, 'pending_review',
   JSON_OBJECT('1', '括号匹配与递归调用模拟', '2', JSON_ARRAY('前序遍历先访问根节点', '后序遍历最后访问根节点', '层序遍历通常借助队列实现'), '3', '先进先出', '4', '递归进入左子树时调用帧不断入栈，访问完成后逐层返回，再进入右子树。'),
   60, '主观题待教师批改', DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR)),
  (@oj_assignment_id, 9001, 'pending_review',
   JSON_OBJECT('1', JSON_OBJECT('language', 'cpp', 'sourceCode', '#include <bits/stdc++.h>\\nusing namespace std;\\nint main(){return 0;}'), '2', JSON_OBJECT('language', 'cpp', 'sourceCode', '#include <bits/stdc++.h>\\nusing namespace std;\\nint main(){return 0;}')),
   0, 'OJ 编程题待教师审核', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR));

DELETE FROM academy_exam_submissions
WHERE exam_id IN (SELECT id FROM academy_exams WHERE exam_code = 'teacher-demo-midterm-review-2026');
DELETE FROM academy_exams WHERE exam_code = 'teacher-demo-midterm-review-2026';

INSERT INTO academy_exams
  (exam_code, course_resource_type, course_id, course_title, exam_title, teacher_name,
   exam_status, starts_at, deadline_at, attempts_limit, duration_minutes, total_score, exam_description)
VALUES
  ('teacher-demo-midterm-review-2026', 'online-open-courses', 'teacher-demo-data-structure-2026',
   '数据结构可视化与算法实践', '数据结构阶段测验主观题批改', '陈思远',
   '正在进行', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 4 DAY), 1, 75, 100,
   '包含客观题和主观题，用于展示教师工作台中的未批改考试数量。');

SET @demo_exam_id := (SELECT id FROM academy_exams WHERE exam_code = 'teacher-demo-midterm-review-2026' LIMIT 1);

INSERT INTO academy_exam_questions
  (exam_id, question_order, question_type, question_label, question_title, question_options,
   placeholder_text, score, correct_answer, answer_explanation, auto_gradable, oj_problem_id, requires_teacher_review)
VALUES
  (@demo_exam_id, 1, 'single', '第 1 题', 'BFS 通常使用的数据结构是？',
   JSON_ARRAY('栈', '队列', '堆', '哈希表'), NULL, 20, JSON_QUOTE('队列'), 'BFS 按层扩展，通常使用队列。', 1, NULL, 0),
  (@demo_exam_id, 2, 'short', '第 2 题', '请说明图的连通块计数中 visited 数组的作用。',
   JSON_ARRAY(), '结合一次 DFS/BFS 的过程说明。', 80, NULL, '需说明避免重复访问与识别新连通块。', 0, NULL, 1);

INSERT INTO academy_exam_submissions
  (exam_id, user_id, submission_status, answer_payload, score, teacher_feedback, started_at, submitted_at, created_at)
VALUES
  (@demo_exam_id, 9001, 'pending_review',
   JSON_OBJECT('1', '队列', '2', 'visited 用来标记已经访问过的节点，避免在无向边中重复遍历；当外层循环遇到未访问节点时，说明发现新的连通块。'),
   20, '主观题待教师批改', DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR));
