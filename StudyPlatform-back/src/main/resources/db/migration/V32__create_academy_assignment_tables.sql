CREATE TABLE IF NOT EXISTS academy_assignments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  assignment_code VARCHAR(120) NOT NULL,
  course_resource_type VARCHAR(64) NOT NULL,
  course_id VARCHAR(120) NOT NULL,
  assignment_title VARCHAR(255) NOT NULL,
  teacher_name VARCHAR(120) DEFAULT NULL,
  assignment_status VARCHAR(32) NOT NULL DEFAULT '正在进行',
  deadline_at DATETIME DEFAULT NULL,
  attempts_limit INT NOT NULL DEFAULT 1,
  duration_minutes INT DEFAULT NULL,
  total_score INT NOT NULL DEFAULT 100,
  assignment_description TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_academy_assignments_code (assignment_code),
  KEY idx_academy_assignments_course (course_resource_type, course_id),
  KEY idx_academy_assignments_status (assignment_status)
);

CREATE TABLE IF NOT EXISTS academy_assignment_questions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  assignment_id BIGINT NOT NULL,
  question_order INT NOT NULL,
  question_type VARCHAR(32) NOT NULL,
  question_label VARCHAR(64) DEFAULT NULL,
  question_title TEXT NOT NULL,
  question_options JSON DEFAULT NULL,
  placeholder_text TEXT,
  score INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_assignment_question_order (assignment_id, question_order),
  CONSTRAINT fk_assignment_questions_assignment
    FOREIGN KEY (assignment_id) REFERENCES academy_assignments(id)
    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS academy_assignment_submissions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  assignment_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL DEFAULT 1,
  submission_status VARCHAR(32) NOT NULL DEFAULT 'draft',
  answer_payload JSON DEFAULT NULL,
  score INT DEFAULT NULL,
  teacher_feedback TEXT,
  submitted_at DATETIME DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_assignment_submissions_user (user_id, submission_status),
  KEY idx_assignment_submissions_assignment (assignment_id),
  CONSTRAINT fk_assignment_submissions_assignment
    FOREIGN KEY (assignment_id) REFERENCES academy_assignments(id)
    ON DELETE CASCADE
);
