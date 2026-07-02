CREATE TABLE IF NOT EXISTS academy_course_enrollments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_type VARCHAR(64) NOT NULL,
  course_id VARCHAR(128) NOT NULL,
  user_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_academy_course_enrollment (resource_type, course_id, user_id)
);

CREATE TABLE IF NOT EXISTS academy_course_reviews (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_type VARCHAR(64) NOT NULL,
  course_id VARCHAR(128) NOT NULL,
  user_name VARCHAR(64) NOT NULL,
  rating TINYINT NOT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_academy_course_reviews_course (resource_type, course_id, created_at)
);

INSERT INTO academy_course_reviews (resource_type, course_id, user_name, rating, content, created_at)
VALUES
  ('online-open-courses', '46004_1476538444', '程序设计学习者', 5, '课程讲解清楚，适合继续巩固 C 语言基础，尤其是函数和指针部分很实用。', '2026-07-01 10:15:00'),
  ('online-open-courses', '46004_1476538444', 'BIT 学生', 5, '案例比较完整，跟着练习能明显提升调试和读代码能力。', '2026-07-01 14:30:00'),
  ('online-open-courses', '46004_1476538444', '自学用户', 4, '希望后续能补充更多综合项目，不过当前内容已经很适合入门进阶。', '2026-07-02 09:05:00');
