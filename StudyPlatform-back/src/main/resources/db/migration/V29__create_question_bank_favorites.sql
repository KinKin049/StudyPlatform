CREATE TABLE IF NOT EXISTS course_question_bank_favorites (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL DEFAULT 1,
  question_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_course_question_bank_favorites_user_question (user_id, question_id),
  KEY idx_course_question_bank_favorites_user_created (user_id, created_at),
  KEY idx_course_question_bank_favorites_question (question_id),
  CONSTRAINT fk_course_question_bank_favorites_question
    FOREIGN KEY (question_id) REFERENCES course_question_bank_questions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
