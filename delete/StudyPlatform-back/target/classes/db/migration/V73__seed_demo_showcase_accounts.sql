-- 第七部分功能展示专用演示账号与数据。
-- 固定使用 9001/9002/9003 三个高位用户 ID，避免影响日常注册账号。

DELETE FROM oj_submission_cases
WHERE submission_id IN (SELECT id FROM oj_submissions WHERE user_id IN (9001, 9002, 9003));
DELETE FROM oj_submissions WHERE user_id IN (9001, 9002, 9003);
DELETE FROM academy_exam_submissions WHERE user_id IN (9001, 9002, 9003);
DELETE FROM academy_assignment_submissions WHERE user_id IN (9001, 9002, 9003);
DELETE FROM academy_textbook_order_items
WHERE order_id IN (SELECT id FROM academy_textbook_orders WHERE user_id IN (9001, 9002, 9003));
DELETE FROM academy_textbook_orders WHERE user_id IN (9001, 9002, 9003);
DELETE FROM academy_textbook_cart_items WHERE user_id IN (9001, 9002, 9003);
DELETE FROM academy_textbook_reviews WHERE user_id IN (9001, 9002, 9003);
DELETE FROM academy_course_reviews WHERE user_id IN (9001, 9002, 9003) OR reply_user_id IN (9001, 9002, 9003);
DELETE FROM academy_course_enrollments WHERE user_id IN (9001, 9002, 9003);
DELETE FROM course_question_bank_favorites WHERE user_id IN (9001, 9002, 9003);
DELETE FROM course_question_bank_mistakes WHERE user_id IN (9001, 9002, 9003);
DELETE FROM profile_learning_events WHERE user_id IN (9001, 9002, 9003);
DELETE FROM profile_learning_time_records WHERE user_id IN (9001, 9002, 9003);
DELETE FROM game_ladder_jump_records WHERE user_id IN (9001, 9002, 9003);
DELETE FROM game_type_warrior_records WHERE user_id IN (9001, 9002, 9003);
DELETE FROM coin_spend_records WHERE user_id IN (9001, 9002, 9003);
DELETE FROM coin_reward_records WHERE user_id IN (9001, 9002, 9003);
DELETE FROM user_vouchers WHERE user_id IN (9001, 9002, 9003);
DELETE FROM well_log_record WHERE user_id IN (9001, 9002, 9003);
DELETE FROM production_pump_record WHERE user_id IN (9001, 9002, 9003);
DELETE FROM production_reservoir_record WHERE user_id IN (9001, 9002, 9003);
DELETE FROM production_waterflood_record WHERE user_id IN (9001, 9002, 9003);
DELETE FROM production_stimulation_record WHERE user_id IN (9001, 9002, 9003);
DELETE FROM teacher_published_courses WHERE publisher_user_id IN (9001, 9002, 9003);
DELETE FROM profile_user_profiles WHERE user_id IN (9001, 9002, 9003);
DELETE FROM users WHERE id IN (9001, 9002, 9003);

INSERT INTO users
  (id, username, password_hash, nickname, avatar_url, role, enabled, email, role_type,
   learning_goal, interests_json, school, teacher_name, pet_key, agreement_accepted, onboarding_completed)
VALUES
  (9001, 'lin_yutong_2026', '$2a$10$LG5h947z1vUryu5voLEsaeVRnZ0EZhgFenZnyAGplvF7X0nNcSqKK',
   '林雨桐', NULL, 'STUDENT', 1, 'lin.yutong2026@study-demo.com', 'student',
   '在毕业设计展示周期内系统完成英语六级词汇、数据结构、C语言编程与石油工程仿真实验的综合复习，保持每日学习打卡并形成可追踪的学习曲线。',
   JSON_ARRAY('英语四六级', '数据结构', 'C语言程序设计', 'OJ刷题', '石油工程仿真', '教材精读'),
   '中国石油大学（北京）', NULL, 'milu', 1, 1),
  (9002, 'chen_siyuan_teacher', '$2a$10$Iw3KmeF63trALc2AkJzoOeiEps/zU99JDDXvdvkNDYX6StfDg/xri',
   '陈思远', NULL, 'TEACHER', 1, 'chen.siyuan.teacher@study-demo.com', 'teacher',
   '建设课程资源、维护作业考试、跟踪学生评价并通过教师工作台完成课程反馈闭环。',
   JSON_ARRAY('课程建设', '作业设计', '程序设计教学', '学习数据分析'),
   '中国石油大学（北京）', '陈思远', 'aurora', 1, 1),
  (9003, 'zhou_mingrui_admin', '$2a$10$C9BvvAeUC0zTFYUxbuOjkeHtLxyodhIuk8.epZabX9wZ037CUw5Xu',
   '周明睿', NULL, 'ADMIN', 1, 'zhou.mingrui.admin@study-demo.com', 'admin',
   '维护平台用户、课程分类、题库、OJ题目和金币卡券数据，保证第七部分功能展示有完整后台样例。',
   JSON_ARRAY('后台管理', '数据维护', '系统测试', '报告截图'),
   'StudyPlatform 管理中心', '周明睿', 'nova', 1, 1)
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  nickname = VALUES(nickname),
  role = VALUES(role),
  enabled = 1,
  email = VALUES(email),
  role_type = VALUES(role_type),
  learning_goal = VALUES(learning_goal),
  interests_json = VALUES(interests_json),
  school = VALUES(school),
  teacher_name = VALUES(teacher_name),
  pet_key = VALUES(pet_key),
  agreement_accepted = 1,
  onboarding_completed = 1;

INSERT INTO profile_user_profiles
  (user_id, display_name, handle, role_label, bio, location, school, avatar_path, admin_coin_adjustment, admin_data_note)
VALUES
  (9001, '林雨桐', '@lin-yutong', '能源智能学习方向学生',
   '正在准备英语六级、数据结构与程序设计综合复习，同时使用测井解释和采油仿真实验模块完成专业课程实践。过去一个多月基本保持每日学习，课程、题库、OJ、教材和游戏模块均有真实使用记录。',
   '北京', '中国石油大学（北京）', NULL, 1200, '第七部分功能展示学生账号：主页、金币、课程、题库、游戏、实验数据完整。'),
  (9002, '陈思远', '@chen-siyuan', '程序设计课程教师',
   '负责在线开放课程建设、作业考试设计与课程评价回复。该账号用于展示教师发布课程、教师工作台和评价信箱等功能。',
   '北京', '中国石油大学（北京）', NULL, 0, '第七部分功能展示教师账号。'),
  (9003, '周明睿', '@zhou-admin', '平台管理员',
   '负责维护系统用户、课程分类、题库、OJ题目、金币卡券和教材数据。该账号用于展示后台管理功能。',
   '北京', 'StudyPlatform 管理中心', NULL, 0, '第七部分功能展示管理员账号。')
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  handle = VALUES(handle),
  role_label = VALUES(role_label),
  bio = VALUES(bio),
  location = VALUES(location),
  school = VALUES(school),
  avatar_path = VALUES(avatar_path),
  admin_coin_adjustment = VALUES(admin_coin_adjustment),
  admin_data_note = VALUES(admin_data_note);

INSERT INTO online_open_courses
  (external_course_id, course_name, teacher_name, category, school_name, cover_file_path,
   start_time, participant_count, course_comment, course_description, source_url, certified, certification_label)
VALUES
  ('teacher-demo-data-structure-2026', '数据结构可视化与算法实践', '陈思远', '计算机基础',
   '中国石油大学（北京）', '/uploads/demo/data-structure-cover.png', '2026春季',
   328, '教师演示课程：以栈、队列、树、图和排序算法为主线，结合可视化模块与OJ练习完成算法能力训练。',
   '课程包含算法概念导学、课堂演示、课后作业、OJ练习和阶段测验，适合作为第七部分教师发布课程与学生学习流程展示素材。',
   '/academy/open-courses/teacher-demo-data-structure-2026', 1, '教师自建认证课程')
ON DUPLICATE KEY UPDATE
  course_name = VALUES(course_name),
  teacher_name = VALUES(teacher_name),
  category = VALUES(category),
  school_name = VALUES(school_name),
  participant_count = VALUES(participant_count),
  course_comment = VALUES(course_comment),
  course_description = VALUES(course_description),
  certified = VALUES(certified),
  certification_label = VALUES(certification_label);

INSERT INTO teacher_published_courses
  (course_id, publisher_user_id, semester_plan, course_overview, course_detail, video_file_path)
VALUES
  ('teacher-demo-data-structure-2026', 9002,
   '第1-2周：线性表与栈队列；第3-4周：树与二叉树；第5-6周：图与最短路径；第7周：排序算法；第8周：综合OJ训练。',
   '通过可视化演示与在线评测结合，帮助学生把抽象算法过程转化为可观察、可提交、可复盘的学习成果。',
   '课程安排包括课堂导学、动画演示、章节题库、OJ代码提交和阶段考试。教师在工作台中跟踪课程评价、作业提交和考试结果。',
   '/uploads/demo/data-structure-intro.mp4')
ON DUPLICATE KEY UPDATE
  publisher_user_id = VALUES(publisher_user_id),
  semester_plan = VALUES(semester_plan),
  course_overview = VALUES(course_overview),
  course_detail = VALUES(course_detail),
  video_file_path = VALUES(video_file_path);

INSERT INTO academy_course_enrollments (resource_type, course_id, user_id, created_at)
SELECT resource_type, course_id, 9001, enrolled_at
FROM (
  SELECT 'online-open-courses' AS resource_type, external_course_id AS course_id,
         DATE_SUB(NOW(), INTERVAL (ROW_NUMBER() OVER (ORDER BY id) + 24) DAY) AS enrolled_at
  FROM online_open_courses
  ORDER BY id
  LIMIT 6
) seeded
ON DUPLICATE KEY UPDATE created_at = VALUES(created_at);

INSERT INTO academy_course_enrollments (resource_type, course_id, user_id, created_at)
SELECT resource_type, course_id, 9001, enrolled_at
FROM (
  SELECT 'general-courses' AS resource_type, external_course_id AS course_id,
         DATE_SUB(NOW(), INTERVAL (ROW_NUMBER() OVER (ORDER BY id) + 18) DAY) AS enrolled_at
  FROM general_courses
  ORDER BY id
  LIMIT 4
) seeded
ON DUPLICATE KEY UPDATE created_at = VALUES(created_at);

INSERT INTO academy_course_enrollments (resource_type, course_id, user_id, created_at)
SELECT resource_type, course_id, 9001, enrolled_at
FROM (
  SELECT 'micro-major-courses' AS resource_type, external_course_id AS course_id,
         DATE_SUB(NOW(), INTERVAL (ROW_NUMBER() OVER (ORDER BY id) + 12) DAY) AS enrolled_at
  FROM micro_major_courses
  ORDER BY id
  LIMIT 3
) seeded
ON DUPLICATE KEY UPDATE created_at = VALUES(created_at);

INSERT INTO academy_course_enrollments (resource_type, course_id, user_id, created_at)
VALUES ('online-open-courses', 'teacher-demo-data-structure-2026', 9001, DATE_SUB(NOW(), INTERVAL 6 DAY))
ON DUPLICATE KEY UPDATE created_at = VALUES(created_at);

INSERT INTO academy_course_reviews
  (resource_type, course_id, user_id, user_name, rating, content, reply_content, reply_user_id,
   reply_user_name, reply_user_role_type, replied_at, teacher_read_at, created_at)
VALUES
  ('online-open-courses', 'teacher-demo-data-structure-2026', 9001, '林雨桐', 5,
   '可视化演示和OJ练习衔接很自然，特别适合复习树和图的遍历过程。',
   '谢谢反馈，后续会补充更多图算法综合练习。', 9002, '陈思远', 'teacher',
   DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
  ('general-courses', (SELECT external_course_id FROM general_courses ORDER BY id LIMIT 1), 9001, '林雨桐', 5,
   '通识课程的章节组织清楚，配合个人主页的学习时长统计很适合做持续学习记录。',
   NULL, NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 11 DAY));

INSERT INTO profile_learning_time_records
  (user_id, module_type, target_code, target_title, duration_seconds, created_at)
SELECT
  9001,
  CASE MOD(days.n, 7)
    WHEN 0 THEN 'video'
    WHEN 1 THEN 'question_bank'
    WHEN 2 THEN 'oj'
    WHEN 3 THEN 'visualization'
    WHEN 4 THEN 'petroleum'
    WHEN 5 THEN 'assignment'
    ELSE 'exam'
  END,
  CASE MOD(days.n, 7)
    WHEN 0 THEN 'online-open-courses'
    WHEN 1 THEN 'cet6'
    WHEN 2 THEN 'oj-practice'
    WHEN 3 THEN 'data-structure'
    WHEN 4 THEN 'production-simulation'
    WHEN 5 THEN 'assignment-programming'
    ELSE 'exam-stage'
  END,
  CASE MOD(days.n, 7)
    WHEN 0 THEN '在线课程视频学习'
    WHEN 1 THEN '四六级与课程题库练习'
    WHEN 2 THEN 'OJ编程训练'
    WHEN 3 THEN '数据结构可视化学习'
    WHEN 4 THEN '石油工程仿真实验'
    WHEN 5 THEN '课程作业练习'
    ELSE '阶段考试复习'
  END,
  1800 + MOD(days.n * 431, 3600),
  DATE_ADD(DATE_SUB(CURRENT_DATE, INTERVAL days.n DAY), INTERVAL (19 + MOD(days.n, 4)) HOUR)
FROM (
  SELECT ones.i + tens.i * 10 AS n
  FROM (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
  CROSS JOIN (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) tens
  WHERE ones.i + tens.i * 10 < 45
) days;

INSERT INTO profile_learning_events
  (user_id, event_type, set_code, question_id, question_type, selected_answer, correct_answer,
   is_correct, vocabulary_status, created_at)
SELECT
  9001,
  CASE WHEN ranked.question_type = 'vocabulary' THEN 'vocabulary' ELSE 'answer' END,
  ranked.set_code,
  ranked.id,
  ranked.question_type,
  CASE WHEN ranked.question_type = 'vocabulary' THEN NULL ELSE 'A' END,
  ranked.answer,
  CASE WHEN ranked.question_type = 'vocabulary' THEN NULL WHEN MOD(ranked.rn, 5) = 0 THEN 0 ELSE 1 END,
  CASE WHEN ranked.question_type = 'vocabulary'
       THEN CASE WHEN MOD(ranked.rn, 4) = 0 THEN 'reviewing' ELSE 'known' END
       ELSE NULL END,
  DATE_ADD(DATE_SUB(CURRENT_DATE, INTERVAL MOD(ranked.rn, 42) DAY), INTERVAL (8 + MOD(ranked.rn, 11)) HOUR)
FROM (
  SELECT q.id, q.question_type, q.answer, s.set_code, ROW_NUMBER() OVER (ORDER BY s.sort_order, q.id) AS rn
  FROM course_question_bank_questions q
  JOIN course_question_bank_sets s ON s.id = q.set_id
  ORDER BY s.sort_order, q.id
  LIMIT 120
) ranked;

INSERT INTO course_question_bank_mistakes
  (user_id, question_id, selected_answer, correct_answer, wrong_count, correct_streak,
   mastered, first_wrong_at, last_wrong_at, last_reviewed_at, created_at)
SELECT
  9001,
  ranked.id,
  'A',
  ranked.answer,
  1 + MOD(ranked.rn, 4),
  MOD(ranked.rn, 3),
  CASE WHEN MOD(ranked.rn, 5) = 0 THEN 1 ELSE 0 END,
  DATE_SUB(NOW(), INTERVAL (ranked.rn + 21) DAY),
  DATE_SUB(NOW(), INTERVAL (ranked.rn + 7) DAY),
  DATE_SUB(NOW(), INTERVAL ranked.rn DAY),
  DATE_SUB(NOW(), INTERVAL (ranked.rn + 21) DAY)
FROM (
  SELECT q.id, q.answer, ROW_NUMBER() OVER (ORDER BY q.id) AS rn
  FROM course_question_bank_questions q
  WHERE q.question_type <> 'vocabulary'
  ORDER BY q.id
  LIMIT 18
) ranked
ON DUPLICATE KEY UPDATE
  selected_answer = VALUES(selected_answer),
  correct_answer = VALUES(correct_answer),
  wrong_count = VALUES(wrong_count),
  correct_streak = VALUES(correct_streak),
  mastered = VALUES(mastered),
  last_wrong_at = VALUES(last_wrong_at),
  last_reviewed_at = VALUES(last_reviewed_at);

INSERT INTO course_question_bank_favorites (user_id, question_id, created_at)
SELECT 9001, ranked.id, DATE_SUB(NOW(), INTERVAL ranked.rn DAY)
FROM (
  SELECT q.id, ROW_NUMBER() OVER (ORDER BY q.id DESC) AS rn
  FROM course_question_bank_questions q
  ORDER BY q.id DESC
  LIMIT 24
) ranked
ON DUPLICATE KEY UPDATE created_at = VALUES(created_at);

INSERT INTO game_ladder_jump_records
  (user_id, question_bank_code, total_coins, correct_count, wrong_count, duration_seconds, created_at)
VALUES
  (9001, 'cet4', 86, 42, 6, 780, DATE_SUB(NOW(), INTERVAL 12 DAY)),
  (9001, 'cet6', 118, 55, 8, 960, DATE_SUB(NOW(), INTERVAL 5 DAY)),
  (9001, 'modern-history', 72, 36, 5, 640, DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO game_type_warrior_records
  (user_id, reached_wave, completed_wave_count, score, max_combo, solved_word_count,
   total_kill_count, typed_letter_count, duration_seconds, effective_typing_seconds, created_at)
VALUES
  (9001, 9, 8, 5680, 37, 86, 91, 540, 510, 472, DATE_SUB(NOW(), INTERVAL 9 DAY)),
  (9001, 12, 11, 8240, 52, 124, 133, 780, 720, 651, DATE_SUB(NOW(), INTERVAL 3 DAY));

INSERT INTO coin_reward_records
  (user_id, source_type, source_key, reason, amount, reference_id, created_at)
VALUES
  (9001, 'demo_seed', 'showcase-comprehensive-learning-2026', '连续学习、课程实践与综合展示奖励', 6200, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
  (9002, 'demo_seed', 'teacher-course-build-2026', '教师课程建设与评价回复展示奖励', 1800, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
  (9003, 'demo_seed', 'admin-maintenance-2026', '后台管理演示数据维护奖励', 1600, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY))
ON DUPLICATE KEY UPDATE amount = VALUES(amount), reason = VALUES(reason), created_at = VALUES(created_at);

INSERT IGNORE INTO coin_reward_records
  (user_id, source_type, source_key, reason, amount, reference_id, created_at)
SELECT user_id, 'demo_learning_time', CONCAT('time:', id), CONCAT(target_title, '奖励'),
       GREATEST(1, FLOOR(duration_seconds / 600) * 3), id, created_at
FROM profile_learning_time_records
WHERE user_id = 9001;

INSERT IGNORE INTO coin_reward_records
  (user_id, source_type, source_key, reason, amount, reference_id, created_at)
SELECT user_id, 'demo_learning_event', CONCAT('event:', id),
       CASE WHEN event_type = 'vocabulary' THEN '词汇掌握奖励' ELSE '题库答题奖励' END,
       CASE WHEN event_type = 'vocabulary' AND vocabulary_status = 'known' THEN 2
            WHEN event_type = 'answer' AND is_correct = 1 THEN 2
            ELSE 1 END,
       id, created_at
FROM profile_learning_events
WHERE user_id = 9001;

INSERT IGNORE INTO coin_reward_records
  (user_id, source_type, source_key, reason, amount, reference_id, created_at)
SELECT user_id, 'demo_game', CONCAT('ladder:', id), '万题天梯跳成绩奖励', total_coins, id, created_at
FROM game_ladder_jump_records
WHERE user_id = 9001;

INSERT IGNORE INTO coin_reward_records
  (user_id, source_type, source_key, reason, amount, reference_id, created_at)
SELECT user_id, 'demo_game', CONCAT('type-warrior:', id), 'Type Warrior 成绩奖励', ROUND(score / 100), id, created_at
FROM game_type_warrior_records
WHERE user_id = 9001;

INSERT INTO user_vouchers
  (user_id, voucher_key, voucher_type, name, description, quantity)
SELECT 9001, voucher_key, voucher_type, name, description, 3
FROM voucher_items
WHERE enabled = 1
ON DUPLICATE KEY UPDATE
  voucher_type = VALUES(voucher_type),
  name = VALUES(name),
  description = VALUES(description),
  quantity = VALUES(quantity);

INSERT INTO academy_textbook_cart_items (user_id, textbook_id, quantity, created_at, updated_at)
SELECT 9001, external_textbook_id, 1 + MOD(ROW_NUMBER() OVER (ORDER BY id), 2),
       DATE_SUB(NOW(), INTERVAL (ROW_NUMBER() OVER (ORDER BY id) + 2) DAY),
       DATE_SUB(NOW(), INTERVAL (ROW_NUMBER() OVER (ORDER BY id) + 1) DAY)
FROM excellent_textbooks
ORDER BY id
LIMIT 4
ON DUPLICATE KEY UPDATE quantity = VALUES(quantity), updated_at = VALUES(updated_at);

INSERT INTO academy_textbook_orders
  (user_id, order_no, total_amount, original_amount, discount_amount, voucher_key, voucher_name, order_status, created_at)
SELECT
  9001,
  CONCAT('DEMO-TB-', external_textbook_id),
  49.80,
  64.80,
  15.00,
  'coupon-textbook-80-15',
  '满 80 元减 15 元优惠券',
  CASE WHEN ROW_NUMBER() OVER (ORDER BY id) = 1 THEN '已完成' ELSE '已支付' END,
  DATE_SUB(NOW(), INTERVAL (ROW_NUMBER() OVER (ORDER BY id) + 8) DAY)
FROM excellent_textbooks
ORDER BY id
LIMIT 3
ON DUPLICATE KEY UPDATE
  total_amount = VALUES(total_amount),
  original_amount = VALUES(original_amount),
  discount_amount = VALUES(discount_amount),
  voucher_key = VALUES(voucher_key),
  voucher_name = VALUES(voucher_name),
  order_status = VALUES(order_status),
  created_at = VALUES(created_at);

INSERT INTO academy_textbook_order_items
  (order_id, textbook_id, textbook_name, unit_price, quantity)
SELECT o.id, t.external_textbook_id, t.textbook_name, COALESCE(d.discount_price, 49.80), 1
FROM academy_textbook_orders o
JOIN excellent_textbooks t ON o.order_no = CONCAT('DEMO-TB-', t.external_textbook_id)
LEFT JOIN academy_textbook_details d ON d.textbook_id = t.external_textbook_id
WHERE o.user_id = 9001;

INSERT INTO academy_textbook_reviews
  (user_id, textbook_id, user_name, rating, content, reply_content, reply_user_id,
   reply_user_name, reply_user_role_type, replied_at, created_at)
SELECT 9001, external_textbook_id, '林雨桐', 5,
       CONCAT('教材《', textbook_name, '》章节结构清晰，适合配合在线课程做系统复习。'),
       '感谢评价，后续会继续补充教材配套资源。', 9003, '周明睿', 'admin',
       DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)
FROM excellent_textbooks
ORDER BY id
LIMIT 2;

INSERT INTO oj_submissions
  (problem_id, user_id, language, source_code, status, score, time_used_ms, memory_used_kb,
   message, judged_at, created_at)
SELECT id, 9001, 'java',
       'public class Main { public static void main(String[] args) { System.out.println("accepted"); } }',
       'ACCEPTED', 100, 38 + ROW_NUMBER() OVER (ORDER BY id), 15600,
       '演示账号提交通过，全部测试用例通过。', DATE_SUB(NOW(), INTERVAL ROW_NUMBER() OVER (ORDER BY id) DAY),
       DATE_SUB(NOW(), INTERVAL ROW_NUMBER() OVER (ORDER BY id) DAY)
FROM oj_problems
WHERE status = 'PUBLISHED'
ORDER BY id
LIMIT 5;

INSERT INTO oj_submission_cases
  (submission_id, test_case_id, status, time_used_ms, memory_used_kb, message, created_at)
SELECT s.id, tc.id, 'ACCEPTED', 12, 8200, '样例通过', s.created_at
FROM oj_submissions s
JOIN oj_test_cases tc ON tc.problem_id = s.problem_id
WHERE s.user_id = 9001;

INSERT INTO academy_assignment_submissions
  (assignment_id, user_id, submission_status, answer_payload, score, teacher_feedback, submitted_at, created_at)
SELECT id, 9001, 'submitted',
       JSON_OBJECT('single', 'A', 'blank', 'void', 'short', '能够说明核心思路并给出关键步骤。'),
       92, '完成度较高，编程题可继续优化边界条件。', DATE_SUB(NOW(), INTERVAL 4 DAY),
       DATE_SUB(NOW(), INTERVAL 4 DAY)
FROM academy_assignments
ORDER BY id
LIMIT 2;

INSERT INTO academy_exam_submissions
  (exam_id, user_id, submission_status, answer_payload, score, teacher_feedback, started_at, submitted_at, created_at)
SELECT id, 9001, 'submitted',
       JSON_OBJECT('single', 'A', 'multiple', JSON_ARRAY('函数职责单一', '命名清晰'), 'short', '结合课程内容完成作答。'),
       88, '阶段测验成绩良好，建议继续复习错题。',
       DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM academy_exams
ORDER BY id
LIMIT 2;

INSERT INTO well_log_record
  (user_id, porosity, oil_saturation, report_json, create_time)
VALUES
  (9001, 0.21, 0.66, JSON_OBJECT('conclusion', '解释结果显示中高孔、中高含油饱和度，建议作为有利层段展示。', 'layer', '砂岩储层'), DATE_SUB(NOW(), INTERVAL 10 DAY)),
  (9001, 0.18, 0.58, JSON_OBJECT('conclusion', '该层段储层物性中等，需结合自然伽马曲线进一步确认。', 'layer', '砂泥岩过渡层'), DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO production_pump_record
  (user_id, stroke, stroke_times, pump_diameter, work_condition, indicator_chart_data, create_time)
VALUES
  (9001, 3.2, 6.0, 44, '正常', JSON_ARRAY(12, 18, 23, 30, 26, 18, 12), DATE_SUB(NOW(), INTERVAL 8 DAY));

INSERT INTO production_reservoir_record
  (user_id, formation_pressure, permeability, water_saturation, viscosity, daily_oil, daily_water, create_time)
VALUES
  (9001, 18.5, 72.0, 0.34, 8.6, 38.4, 12.7, DATE_SUB(NOW(), INTERVAL 7 DAY));

INSERT INTO production_waterflood_record
  (user_id, injection_rate, effect_day, water_breakthrough_day, peak_oil, production_curve, create_time)
VALUES
  (9001, 120.0, 38, 96, 46.8, JSON_ARRAY(22, 28, 36, 44, 46, 41, 35), DATE_SUB(NOW(), INTERVAL 6 DAY));

INSERT INTO production_stimulation_record
  (user_id, type, sand_volume, displacement, acid_volume, fracture_length, stimulation_ratio, create_time)
VALUES
  (9001, '压裂', 18.5, 4.2, NULL, 126.0, 1.74, DATE_SUB(NOW(), INTERVAL 5 DAY));
