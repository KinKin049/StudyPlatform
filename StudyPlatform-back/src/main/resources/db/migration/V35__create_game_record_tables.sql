CREATE TABLE IF NOT EXISTS game_ladder_jump_records (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL DEFAULT 1,
  question_bank_code VARCHAR(64) NULL,
  total_coins INT NOT NULL DEFAULT 0,
  correct_count INT NOT NULL DEFAULT 0,
  wrong_count INT NOT NULL DEFAULT 0,
  duration_seconds DOUBLE NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_game_ladder_jump_records_user_created (user_id, created_at),
  KEY idx_game_ladder_jump_records_bank (question_bank_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS game_type_warrior_records (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL DEFAULT 1,
  reached_wave INT NOT NULL DEFAULT 0,
  completed_wave_count INT NOT NULL DEFAULT 0,
  score BIGINT NOT NULL DEFAULT 0,
  max_combo INT NOT NULL DEFAULT 0,
  solved_word_count INT NOT NULL DEFAULT 0,
  total_kill_count INT NOT NULL DEFAULT 0,
  typed_letter_count INT NOT NULL DEFAULT 0,
  duration_seconds DOUBLE NOT NULL DEFAULT 0,
  effective_typing_seconds DOUBLE NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_game_type_warrior_records_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
