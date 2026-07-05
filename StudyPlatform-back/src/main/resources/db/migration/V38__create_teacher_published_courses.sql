CREATE TABLE IF NOT EXISTS teacher_published_courses (
  id BIGINT NOT NULL AUTO_INCREMENT,
  course_id VARCHAR(128) NOT NULL,
  publisher_user_id BIGINT NOT NULL,
  semester_plan VARCHAR(512) NULL,
  course_overview TEXT NULL,
  course_detail TEXT NULL,
  video_file_path VARCHAR(512) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_teacher_published_courses_course (course_id),
  KEY idx_teacher_published_courses_user (publisher_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
