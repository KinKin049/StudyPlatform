CREATE TABLE IF NOT EXISTS course_question_bank_mistakes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL DEFAULT 1,
  question_id BIGINT NOT NULL,
  selected_answer TEXT NULL,
  correct_answer TEXT NULL,
  wrong_count INT NOT NULL DEFAULT 0,
  correct_streak INT NOT NULL DEFAULT 0,
  mastered TINYINT(1) NOT NULL DEFAULT 0,
  first_wrong_at DATETIME NULL,
  last_wrong_at DATETIME NULL,
  last_reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_course_question_bank_mistakes_user_question (user_id, question_id),
  KEY idx_course_question_bank_mistakes_user_mastered (user_id, mastered, updated_at),
  KEY idx_course_question_bank_mistakes_question (question_id),
  CONSTRAINT fk_course_question_bank_mistakes_question
    FOREIGN KEY (question_id) REFERENCES course_question_bank_questions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
