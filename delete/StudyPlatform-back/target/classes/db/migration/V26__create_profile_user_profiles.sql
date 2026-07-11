CREATE TABLE IF NOT EXISTS profile_user_profiles (
  user_id BIGINT NOT NULL,
  display_name VARCHAR(64) NOT NULL DEFAULT 'Kinkin',
  handle VARCHAR(64) NOT NULL DEFAULT '@study-platform',
  role_label VARCHAR(128) NOT NULL DEFAULT 'StudyPlatform 学习者',
  bio VARCHAR(512) NOT NULL DEFAULT '在题库、课程、实验与背单词之间来回穿梭，把零散练习沉淀成稳定的学习曲线。',
  location VARCHAR(64) NOT NULL DEFAULT 'China',
  school VARCHAR(128) NOT NULL DEFAULT 'StudyPlatform',
  avatar_path VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO profile_user_profiles
  (user_id, display_name, handle, role_label, bio, location, school, avatar_path)
VALUES
  (1, 'Kinkin', '@study-platform', 'StudyPlatform 学习者',
   '在题库、课程、实验与背单词之间来回穿梭，把零散练习沉淀成稳定的学习曲线。',
   'China', 'StudyPlatform', NULL)
ON DUPLICATE KEY UPDATE
  display_name = display_name;
