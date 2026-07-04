CREATE TABLE IF NOT EXISTS profile_learning_events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL DEFAULT 1,
  event_type VARCHAR(32) NOT NULL,
  set_code VARCHAR(64) NULL,
  question_id BIGINT NULL,
  question_type VARCHAR(32) NULL,
  selected_answer TEXT NULL,
  correct_answer TEXT NULL,
  is_correct TINYINT(1) NULL,
  vocabulary_status VARCHAR(32) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_profile_learning_events_user_created (user_id, created_at),
  KEY idx_profile_learning_events_user_set (user_id, set_code),
  KEY idx_profile_learning_events_question (question_id),
  CONSTRAINT fk_profile_learning_events_question
    FOREIGN KEY (question_id) REFERENCES course_question_bank_questions (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
