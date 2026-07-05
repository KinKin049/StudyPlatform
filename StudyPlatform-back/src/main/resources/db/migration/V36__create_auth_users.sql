CREATE TABLE IF NOT EXISTS auth_users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  email VARCHAR(128) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role_type VARCHAR(24) NULL,
  learning_goal VARCHAR(255) NULL,
  interests_json JSON NULL,
  school VARCHAR(128) NULL,
  teacher_name VARCHAR(64) NULL,
  pet_key VARCHAR(32) NULL,
  agreement_accepted TINYINT(1) NOT NULL DEFAULT 0,
  onboarding_completed TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_users_username (username),
  UNIQUE KEY uk_auth_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
