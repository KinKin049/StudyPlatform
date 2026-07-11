ALTER TABLE academy_assignment_questions
  ADD COLUMN correct_answer JSON DEFAULT NULL,
  ADD COLUMN answer_explanation TEXT,
  ADD COLUMN auto_gradable TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN oj_problem_id BIGINT DEFAULT NULL,
  ADD COLUMN requires_teacher_review TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE academy_assignments
  ADD COLUMN course_title VARCHAR(255) DEFAULT NULL AFTER course_id;

INSERT INTO academy_assignments
  (assignment_code, course_resource_type, course_id, course_title, assignment_title, teacher_name,
   assignment_status, deadline_at, attempts_limit, duration_minutes, total_score, assignment_description)
VALUES
  ('c-function-practice', 'online-open-courses', '46004_1476538444', 'C语言程序设计（下）', '第 3 章函数练习', '余月',
   '正在进行', '2026-07-10 23:59:00', 3, 45, 100,
   '围绕函数定义、参数传递、返回值和递归思想完成本次练习，提交后系统会记录你的答题情况。'),
  ('labor-value-discussion', 'general-courses', 'general-labor-001', '劳动通论', '专题讨论：劳动价值', '课程团队',
   '正在进行', '2026-07-09 22:00:00', 1, 30, 100,
   '结合课程材料，围绕新时代劳动价值展开观点表达，提交后进入教师互评流程。'),
  ('data-cleaning-report', 'micro-major-courses', 'micro-data-001', '数据分析微专业', '数据清洗项目报告', '项目导师',
   '已结束', '2026-07-03 18:00:00', 0, NULL, 100,
   '本作业用于展示数据清洗流程、异常值处理、缺失值分析和结果复盘。')
ON DUPLICATE KEY UPDATE
  course_title = VALUES(course_title),
  assignment_title = VALUES(assignment_title),
  teacher_name = VALUES(teacher_name),
  assignment_status = VALUES(assignment_status),
  deadline_at = VALUES(deadline_at),
  attempts_limit = VALUES(attempts_limit),
  duration_minutes = VALUES(duration_minutes),
  total_score = VALUES(total_score),
  assignment_description = VALUES(assignment_description);

INSERT INTO academy_assignment_questions
  (assignment_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT a.id, 1, 'single', '单选题',
       '下列关于 C 语言函数返回值的说法，正确的是哪一项？',
       JSON_ARRAY('函数必须返回 int 类型', 'void 函数不能使用 return 语句', '函数返回值类型应与函数声明保持一致', '函数只能返回基本数据类型'),
       NULL, 10, JSON_QUOTE('函数返回值类型应与函数声明保持一致'),
       '函数返回值类型应与函数声明或定义保持一致。', 1, NULL, 0
FROM academy_assignments a WHERE a.assignment_code = 'c-function-practice'
ON DUPLICATE KEY UPDATE question_title = VALUES(question_title), question_options = VALUES(question_options),
  score = VALUES(score), correct_answer = VALUES(correct_answer), answer_explanation = VALUES(answer_explanation),
  auto_gradable = VALUES(auto_gradable), oj_problem_id = VALUES(oj_problem_id), requires_teacher_review = VALUES(requires_teacher_review);

INSERT INTO academy_assignment_questions
  (assignment_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT a.id, 2, 'multiple', '多选题',
       '关于函数参数传递，下列说法正确的有：',
       JSON_ARRAY('形参只在函数调用期间有效', '实参和形参可以同名', '数组名作为参数时通常传递首元素地址', '值传递会直接修改调用处变量本身'),
       NULL, 15, JSON_ARRAY('形参只在函数调用期间有效', '实参和形参可以同名', '数组名作为参数时通常传递首元素地址'),
       '多选题需要完全匹配正确选项，少选或多选均不得分。', 1, NULL, 0
FROM academy_assignments a WHERE a.assignment_code = 'c-function-practice'
ON DUPLICATE KEY UPDATE question_title = VALUES(question_title), question_options = VALUES(question_options),
  score = VALUES(score), correct_answer = VALUES(correct_answer), answer_explanation = VALUES(answer_explanation),
  auto_gradable = VALUES(auto_gradable), oj_problem_id = VALUES(oj_problem_id), requires_teacher_review = VALUES(requires_teacher_review);

INSERT INTO academy_assignment_questions
  (assignment_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT a.id, 3, 'blank', '填空题',
       '如果函数没有返回值，函数返回类型通常写作 ______。',
       NULL, '请输入答案', 10, JSON_ARRAY('void'),
       'C 语言中无返回值函数通常声明为 void。', 1, NULL, 0
FROM academy_assignments a WHERE a.assignment_code = 'c-function-practice'
ON DUPLICATE KEY UPDATE question_title = VALUES(question_title), placeholder_text = VALUES(placeholder_text),
  score = VALUES(score), correct_answer = VALUES(correct_answer), answer_explanation = VALUES(answer_explanation),
  auto_gradable = VALUES(auto_gradable), requires_teacher_review = VALUES(requires_teacher_review);

INSERT INTO academy_assignment_questions
  (assignment_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT a.id, 4, 'short', '简答题',
       '请简要说明“函数封装”对程序设计的意义。',
       NULL, '从代码复用、结构清晰、调试维护等角度作答', 20, NULL,
       NULL, 0, NULL, 1
FROM academy_assignments a WHERE a.assignment_code = 'c-function-practice'
ON DUPLICATE KEY UPDATE question_title = VALUES(question_title), placeholder_text = VALUES(placeholder_text),
  score = VALUES(score), auto_gradable = VALUES(auto_gradable), requires_teacher_review = VALUES(requires_teacher_review);

INSERT INTO academy_assignment_questions
  (assignment_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT a.id, 5, 'code', '编程题',
       '编写一个函数 maxOfThree，返回三个整数中的最大值。',
       NULL, 'int maxOfThree(int a, int b, int c) {\n  // 在这里编写代码\n}', 45, NULL,
       '编程题后续将关联 OJ 判题结果，并进入教师审核。', 0, 1, 1
FROM academy_assignments a WHERE a.assignment_code = 'c-function-practice'
ON DUPLICATE KEY UPDATE question_title = VALUES(question_title), placeholder_text = VALUES(placeholder_text),
  score = VALUES(score), answer_explanation = VALUES(answer_explanation), auto_gradable = VALUES(auto_gradable),
  oj_problem_id = VALUES(oj_problem_id), requires_teacher_review = VALUES(requires_teacher_review);

INSERT INTO academy_assignment_questions
  (assignment_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT a.id, 1, 'short', '论述题',
       '结合自身专业学习，谈谈你如何理解劳动创造价值。',
       NULL, '请写出不少于 150 字的观点', 60, NULL, NULL, 0, NULL, 1
FROM academy_assignments a WHERE a.assignment_code = 'labor-value-discussion'
ON DUPLICATE KEY UPDATE question_title = VALUES(question_title), placeholder_text = VALUES(placeholder_text),
  score = VALUES(score), requires_teacher_review = VALUES(requires_teacher_review);

INSERT INTO academy_assignment_questions
  (assignment_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT a.id, 2, 'multiple', '多选题',
       '以下哪些属于劳动素养的重要体现？',
       JSON_ARRAY('尊重劳动成果', '具备协作意识', '重视实践能力', '只关注理论成绩'),
       NULL, 20, JSON_ARRAY('尊重劳动成果', '具备协作意识', '重视实践能力'),
       '劳动素养强调尊重劳动、协作意识与实践能力。', 1, NULL, 0
FROM academy_assignments a WHERE a.assignment_code = 'labor-value-discussion'
ON DUPLICATE KEY UPDATE question_title = VALUES(question_title), question_options = VALUES(question_options),
  score = VALUES(score), correct_answer = VALUES(correct_answer), answer_explanation = VALUES(answer_explanation),
  auto_gradable = VALUES(auto_gradable);

INSERT INTO academy_assignment_questions
  (assignment_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT a.id, 3, 'blank', '填空题',
       '劳动教育强调树立正确的劳动观、价值观和 ______。',
       NULL, '请输入答案', 20, JSON_ARRAY('人生观'),
       '劳动教育强调劳动观、价值观和人生观的统一。', 1, NULL, 0
FROM academy_assignments a WHERE a.assignment_code = 'labor-value-discussion'
ON DUPLICATE KEY UPDATE question_title = VALUES(question_title), placeholder_text = VALUES(placeholder_text),
  score = VALUES(score), correct_answer = VALUES(correct_answer), answer_explanation = VALUES(answer_explanation),
  auto_gradable = VALUES(auto_gradable);

INSERT INTO academy_assignment_questions
  (assignment_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT a.id, 1, 'short', '报告说明',
       '请概述你的数据清洗流程。',
       NULL, '说明数据来源、处理步骤和主要结论', 40, NULL, NULL, 0, NULL, 1
FROM academy_assignments a WHERE a.assignment_code = 'data-cleaning-report'
ON DUPLICATE KEY UPDATE question_title = VALUES(question_title), placeholder_text = VALUES(placeholder_text),
  score = VALUES(score), requires_teacher_review = VALUES(requires_teacher_review);

INSERT INTO academy_assignment_questions
  (assignment_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT a.id, 2, 'code', '代码片段',
       '粘贴你用于处理缺失值或异常值的核心代码。',
       NULL, '请粘贴 Python / SQL / R 等代码片段', 60, NULL,
       '编程题后续将关联 OJ 判题结果，并进入教师审核。', 0, NULL, 1
FROM academy_assignments a WHERE a.assignment_code = 'data-cleaning-report'
ON DUPLICATE KEY UPDATE question_title = VALUES(question_title), placeholder_text = VALUES(placeholder_text),
  score = VALUES(score), answer_explanation = VALUES(answer_explanation), requires_teacher_review = VALUES(requires_teacher_review);
