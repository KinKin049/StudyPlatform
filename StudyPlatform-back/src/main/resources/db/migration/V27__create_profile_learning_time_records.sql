CREATE TABLE IF NOT EXISTS profile_learning_time_records (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL DEFAULT 1,
  module_type VARCHAR(32) NOT NULL,
  target_code VARCHAR(128) NULL,
  target_title VARCHAR(128) NULL,
  duration_seconds INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_profile_learning_time_user_module (user_id, module_type),
  KEY idx_profile_learning_time_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO profile_learning_time_records
  (user_id, module_type, target_code, target_title, duration_seconds, created_at)
SELECT 1, 'video', 'online-open-courses', '在线视频学习', 8460, '2026-07-01 20:10:00'
WHERE NOT EXISTS (
  SELECT 1 FROM profile_learning_time_records
  WHERE user_id = 1 AND module_type = 'video'
);

INSERT INTO profile_learning_time_records
  (user_id, module_type, target_code, target_title, duration_seconds, created_at)
SELECT 1, 'visualization', 'data-structure', '可视化模块学习', 5220, '2026-07-02 21:30:00'
WHERE NOT EXISTS (
  SELECT 1 FROM profile_learning_time_records
  WHERE user_id = 1 AND module_type = 'visualization'
);
