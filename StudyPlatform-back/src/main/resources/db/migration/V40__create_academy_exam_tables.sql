CREATE TABLE IF NOT EXISTS academy_exams (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_code VARCHAR(120) NOT NULL,
  course_resource_type VARCHAR(64) NOT NULL,
  course_id VARCHAR(120) NOT NULL,
  course_title VARCHAR(255) DEFAULT NULL,
  exam_title VARCHAR(255) NOT NULL,
  teacher_name VARCHAR(120) DEFAULT NULL,
  exam_status VARCHAR(32) NOT NULL DEFAULT '正在进行',
  starts_at DATETIME DEFAULT NULL,
  deadline_at DATETIME DEFAULT NULL,
  attempts_limit INT NOT NULL DEFAULT 1,
  duration_minutes INT DEFAULT NULL,
  total_score INT NOT NULL DEFAULT 100,
  exam_description TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_academy_exams_code (exam_code),
  KEY idx_academy_exams_course (course_resource_type, course_id),
  KEY idx_academy_exams_status (exam_status),
  KEY idx_academy_exams_time (starts_at, deadline_at)
);

CREATE TABLE IF NOT EXISTS academy_exam_questions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  question_order INT NOT NULL,
  question_type VARCHAR(32) NOT NULL,
  question_label VARCHAR(64) DEFAULT NULL,
  question_title TEXT NOT NULL,
  question_options JSON DEFAULT NULL,
  placeholder_text TEXT,
  score INT NOT NULL DEFAULT 0,
  correct_answer JSON DEFAULT NULL,
  answer_explanation TEXT,
  auto_gradable TINYINT(1) NOT NULL DEFAULT 0,
  oj_problem_id BIGINT DEFAULT NULL,
  requires_teacher_review TINYINT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_exam_question_order (exam_id, question_order),
  CONSTRAINT fk_exam_questions_exam
    FOREIGN KEY (exam_id) REFERENCES academy_exams(id)
    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS academy_exam_submissions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL DEFAULT 1,
  submission_status VARCHAR(32) NOT NULL DEFAULT 'in_progress',
  answer_payload JSON DEFAULT NULL,
  score INT DEFAULT NULL,
  teacher_feedback TEXT,
  started_at DATETIME DEFAULT NULL,
  submitted_at DATETIME DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_exam_submissions_user (user_id, submission_status),
  KEY idx_exam_submissions_exam (exam_id),
  CONSTRAINT fk_exam_submissions_exam
    FOREIGN KEY (exam_id) REFERENCES academy_exams(id)
    ON DELETE CASCADE
);

INSERT INTO academy_exams
  (exam_code, course_resource_type, course_id, course_title, exam_title, teacher_name,
   exam_status, starts_at, deadline_at, attempts_limit, duration_minutes, total_score, exam_description)
VALUES
  ('programming-unit-test', 'online-open-courses', '46004_1476538444', 'C语言程序设计（下）', '程序设计单元测试', '余月',
   '正在进行', '2026-07-01 08:00:00', '2026-07-20 23:59:00', 1, 45, 100,
   '覆盖选择题、填空题与函数编程题，客观题提交后自动批改，编程题提交至 OJ 后等待教师审核。'),
  ('math-stage-quiz', 'general-courses', 'general-math-001', '高等数学', '高等数学阶段测验', '课程团队',
   '即将开始', '2026-07-12 09:00:00', '2026-07-12 10:30:00', 1, 60, 100,
   '围绕极限、导数和微分应用进行阶段性测验，考试开始后会显示倒计时。'),
  ('general-final-exam', 'general-courses', 'general-labor-001', '劳动通论', '通识课程结课考试', '课程团队',
   '已结束', '2026-07-01 09:00:00', '2026-07-01 10:30:00', 0, 90, 100,
   '通识课程结课考试样例，已结束状态用于展示历史考试入口和成绩样式。')
ON DUPLICATE KEY UPDATE
  course_resource_type = VALUES(course_resource_type),
  course_id = VALUES(course_id),
  course_title = VALUES(course_title),
  exam_title = VALUES(exam_title),
  teacher_name = VALUES(teacher_name),
  exam_status = VALUES(exam_status),
  starts_at = VALUES(starts_at),
  deadline_at = VALUES(deadline_at),
  attempts_limit = VALUES(attempts_limit),
  duration_minutes = VALUES(duration_minutes),
  total_score = VALUES(total_score),
  exam_description = VALUES(exam_description);

INSERT INTO academy_exam_questions
  (exam_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT e.id, 1, 'single', '单选题',
       'C 语言中，函数声明的主要作用是什么？',
       JSON_ARRAY('提前告诉编译器函数名称、返回值和参数信息', '自动生成函数体', '强制函数只能被调用一次', '让函数只能返回整数'),
       NULL, 15, JSON_QUOTE('提前告诉编译器函数名称、返回值和参数信息'),
       '函数声明用于让编译器提前获知函数接口信息。', 1, NULL, 0
FROM academy_exams e WHERE e.exam_code = 'programming-unit-test'
ON DUPLICATE KEY UPDATE question_label = VALUES(question_label), question_title = VALUES(question_title),
  question_options = VALUES(question_options), score = VALUES(score), correct_answer = VALUES(correct_answer),
  answer_explanation = VALUES(answer_explanation), auto_gradable = VALUES(auto_gradable),
  oj_problem_id = VALUES(oj_problem_id), requires_teacher_review = VALUES(requires_teacher_review);

INSERT INTO academy_exam_questions
  (exam_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT e.id, 2, 'multiple', '多选题',
       '下列关于函数设计的说法，正确的有：',
       JSON_ARRAY('函数应尽量保持职责单一', '函数名应体现功能含义', '函数参数越多越容易维护', '公共逻辑可通过函数复用'),
       NULL, 20, JSON_ARRAY('函数应尽量保持职责单一', '函数名应体现功能含义', '公共逻辑可通过函数复用'),
       '函数设计强调职责清晰、命名明确和逻辑复用。', 1, NULL, 0
FROM academy_exams e WHERE e.exam_code = 'programming-unit-test'
ON DUPLICATE KEY UPDATE question_label = VALUES(question_label), question_title = VALUES(question_title),
  question_options = VALUES(question_options), score = VALUES(score), correct_answer = VALUES(correct_answer),
  answer_explanation = VALUES(answer_explanation), auto_gradable = VALUES(auto_gradable),
  oj_problem_id = VALUES(oj_problem_id), requires_teacher_review = VALUES(requires_teacher_review);

INSERT INTO academy_exam_questions
  (exam_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT e.id, 3, 'blank', '填空题',
       'C 语言中，不返回任何值的函数返回类型通常写作 ______。',
       NULL, '请输入答案', 15, JSON_ARRAY('void'),
       '无返回值函数通常使用 void 作为返回类型。', 1, NULL, 0
FROM academy_exams e WHERE e.exam_code = 'programming-unit-test'
ON DUPLICATE KEY UPDATE question_label = VALUES(question_label), question_title = VALUES(question_title),
  placeholder_text = VALUES(placeholder_text), score = VALUES(score), correct_answer = VALUES(correct_answer),
  answer_explanation = VALUES(answer_explanation), auto_gradable = VALUES(auto_gradable),
  requires_teacher_review = VALUES(requires_teacher_review);

INSERT INTO academy_exam_questions
  (exam_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT e.id, 4, 'code', '编程题',
       '编写函数 maxOfThree，返回三个整数中的最大值。',
       NULL, 'int maxOfThree(int a, int b, int c) {\n  // 在这里编写代码\n}', 50, NULL,
       '编程题会提交至 OJ 判题，并进入教师审核流程。', 0, NULL, 1
FROM academy_exams e WHERE e.exam_code = 'programming-unit-test'
ON DUPLICATE KEY UPDATE question_label = VALUES(question_label), question_title = VALUES(question_title),
  placeholder_text = VALUES(placeholder_text), score = VALUES(score), answer_explanation = VALUES(answer_explanation),
  auto_gradable = VALUES(auto_gradable), requires_teacher_review = VALUES(requires_teacher_review);

INSERT INTO academy_exam_questions
  (exam_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT e.id, 1, 'single', '单选题',
       '若函数 f(x) 在 x0 处可导，则下列说法正确的是：',
       JSON_ARRAY('f(x) 在 x0 处一定连续', 'f(x) 在 x0 处一定取得极值', 'f(x) 在 x0 处一定不可连续', 'f(x) 在 x0 处导数一定为 0'),
       NULL, 20, JSON_QUOTE('f(x) 在 x0 处一定连续'),
       '可导必连续，但连续不一定可导。', 1, NULL, 0
FROM academy_exams e WHERE e.exam_code = 'math-stage-quiz'
ON DUPLICATE KEY UPDATE question_label = VALUES(question_label), question_title = VALUES(question_title),
  question_options = VALUES(question_options), score = VALUES(score), correct_answer = VALUES(correct_answer),
  answer_explanation = VALUES(answer_explanation), auto_gradable = VALUES(auto_gradable),
  requires_teacher_review = VALUES(requires_teacher_review);

INSERT INTO academy_exam_questions
  (exam_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT e.id, 2, 'multiple', '多选题',
       '以下哪些属于导数的常见应用？',
       JSON_ARRAY('判断函数单调性', '求函数极值', '近似计算函数增量', '直接确定样本方差'),
       NULL, 30, JSON_ARRAY('判断函数单调性', '求函数极值', '近似计算函数增量'),
       '导数常用于单调性、极值和微分近似等问题。', 1, NULL, 0
FROM academy_exams e WHERE e.exam_code = 'math-stage-quiz'
ON DUPLICATE KEY UPDATE question_label = VALUES(question_label), question_title = VALUES(question_title),
  question_options = VALUES(question_options), score = VALUES(score), correct_answer = VALUES(correct_answer),
  answer_explanation = VALUES(answer_explanation), auto_gradable = VALUES(auto_gradable),
  requires_teacher_review = VALUES(requires_teacher_review);

INSERT INTO academy_exam_questions
  (exam_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT e.id, 3, 'short', '解答题',
       '请说明“极限”在导数定义中的作用。',
       NULL, '请写出关键公式和你的理解', 50, NULL,
       NULL, 0, NULL, 1
FROM academy_exams e WHERE e.exam_code = 'math-stage-quiz'
ON DUPLICATE KEY UPDATE question_label = VALUES(question_label), question_title = VALUES(question_title),
  placeholder_text = VALUES(placeholder_text), score = VALUES(score), auto_gradable = VALUES(auto_gradable),
  requires_teacher_review = VALUES(requires_teacher_review);

INSERT INTO academy_exam_questions
  (exam_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT e.id, 1, 'single', '单选题',
       '劳动教育的核心目标之一是帮助学生树立正确的：',
       JSON_ARRAY('劳动观', '消费观', '娱乐观', '流量观'),
       NULL, 40, JSON_QUOTE('劳动观'),
       '劳动教育强调树立正确劳动观。', 1, NULL, 0
FROM academy_exams e WHERE e.exam_code = 'general-final-exam'
ON DUPLICATE KEY UPDATE question_label = VALUES(question_label), question_title = VALUES(question_title),
  question_options = VALUES(question_options), score = VALUES(score), correct_answer = VALUES(correct_answer),
  answer_explanation = VALUES(answer_explanation), auto_gradable = VALUES(auto_gradable),
  requires_teacher_review = VALUES(requires_teacher_review);

INSERT INTO academy_exam_questions
  (exam_id, question_order, question_type, question_label, question_title,
   question_options, placeholder_text, score, correct_answer, answer_explanation,
   auto_gradable, oj_problem_id, requires_teacher_review)
SELECT e.id, 2, 'short', '论述题',
       '结合课程内容，谈谈新时代青年应如何理解劳动价值。',
       NULL, '请结合个人学习经历作答', 60, NULL,
       NULL, 0, NULL, 1
FROM academy_exams e WHERE e.exam_code = 'general-final-exam'
ON DUPLICATE KEY UPDATE question_label = VALUES(question_label), question_title = VALUES(question_title),
  placeholder_text = VALUES(placeholder_text), score = VALUES(score), auto_gradable = VALUES(auto_gradable),
  requires_teacher_review = VALUES(requires_teacher_review);

UPDATE academy_exam_questions q
JOIN academy_exams e ON e.id = q.exam_id
JOIN oj_problems p ON p.slug = 'assignment-max-of-three'
SET q.oj_problem_id = p.id,
    q.answer_explanation = '编程题已关联 OJ 判题，提交后会自动运行测试用例，并进入教师审核。'
WHERE e.exam_code = 'programming-unit-test'
  AND q.question_type = 'code'
  AND q.question_title LIKE '%maxOfThree%';
