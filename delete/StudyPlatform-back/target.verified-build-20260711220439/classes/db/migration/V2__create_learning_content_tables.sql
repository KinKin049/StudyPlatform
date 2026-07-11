CREATE TABLE IF NOT EXISTS learning_content_blocks (
  id BIGINT NOT NULL AUTO_INCREMENT,
  block_code VARCHAR(64) NOT NULL,
  block_name VARCHAR(64) NOT NULL,
  description VARCHAR(255) NULL,
  storage_folder VARCHAR(255) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_learning_content_blocks_code (block_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS online_open_courses (
  id BIGINT NOT NULL AUTO_INCREMENT,
  external_course_id VARCHAR(128) NOT NULL,
  course_name VARCHAR(255) NOT NULL,
  teacher_name VARCHAR(255) NULL,
  category VARCHAR(64) NULL,
  school_name VARCHAR(255) NULL,
  cover_url VARCHAR(1024) NULL,
  cover_file_path VARCHAR(512) NULL,
  start_time VARCHAR(64) NULL,
  participant_count INT NULL,
  course_comment TEXT NULL,
  source_url VARCHAR(1024) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_online_open_courses_external_id (external_course_id),
  KEY idx_online_open_courses_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO learning_content_blocks (block_code, block_name, description, storage_folder, sort_order)
VALUES
  ('online_open_course', '在线开放课程', '在线开放课程内容块', 'online_course', 1),
  ('general_course', '通识课程', '通识课程内容块', 'general_course', 2),
  ('micro_major_course', '微专业课程', '微专业课程内容块', 'micro_major_course', 3),
  ('excellent_textbook', '精品教材', '精品教材内容块', 'excellent_textbook', 4)
ON DUPLICATE KEY UPDATE
  block_name = VALUES(block_name),
  description = VALUES(description),
  storage_folder = VALUES(storage_folder),
  sort_order = VALUES(sort_order);
