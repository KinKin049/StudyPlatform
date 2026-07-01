CREATE TABLE IF NOT EXISTS oj_problems (
  id BIGINT NOT NULL AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  slug VARCHAR(128) NOT NULL,
  description TEXT NOT NULL,
  input_description TEXT NULL,
  output_description TEXT NULL,
  samples JSON NULL,
  difficulty VARCHAR(16) NOT NULL DEFAULT 'EASY',
  time_limit_ms INT NOT NULL DEFAULT 1000,
  memory_limit_kb INT NOT NULL DEFAULT 262144,
  tags JSON NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_oj_problems_slug (slug),
  KEY idx_oj_problems_status (status),
  CONSTRAINT fk_oj_problems_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS oj_test_cases (
  id BIGINT NOT NULL AUTO_INCREMENT,
  problem_id BIGINT NOT NULL,
  input_data MEDIUMTEXT NOT NULL,
  expected_output MEDIUMTEXT NOT NULL,
  sample TINYINT(1) NOT NULL DEFAULT 0,
  weight INT NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_oj_test_cases_problem_id (problem_id),
  CONSTRAINT fk_oj_test_cases_problem_id FOREIGN KEY (problem_id) REFERENCES oj_problems (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS oj_submissions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  problem_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  language VARCHAR(32) NOT NULL,
  source_code MEDIUMTEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  score INT NOT NULL DEFAULT 0,
  time_used_ms INT NULL,
  memory_used_kb INT NULL,
  message VARCHAR(1024) NULL,
  judged_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_oj_submissions_problem_user (problem_id, user_id),
  KEY idx_oj_submissions_status (status),
  CONSTRAINT fk_oj_submissions_problem_id FOREIGN KEY (problem_id) REFERENCES oj_problems (id),
  CONSTRAINT fk_oj_submissions_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS oj_submission_cases (
  id BIGINT NOT NULL AUTO_INCREMENT,
  submission_id BIGINT NOT NULL,
  test_case_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  time_used_ms INT NULL,
  memory_used_kb INT NULL,
  message VARCHAR(1024) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_oj_submission_cases_submission_id (submission_id),
  CONSTRAINT fk_oj_submission_cases_submission_id FOREIGN KEY (submission_id) REFERENCES oj_submissions (id) ON DELETE CASCADE,
  CONSTRAINT fk_oj_submission_cases_test_case_id FOREIGN KEY (test_case_id) REFERENCES oj_test_cases (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
