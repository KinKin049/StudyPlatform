ALTER TABLE profile_user_profiles
  ADD COLUMN admin_coin_adjustment BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN admin_data_note VARCHAR(512) NULL;

ALTER TABLE online_open_courses
  ADD COLUMN certified TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE general_courses
  ADD COLUMN certified TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE micro_major_courses
  ADD COLUMN certified TINYINT(1) NOT NULL DEFAULT 0;

INSERT INTO auth_users
  (username, email, password_hash, role_type, agreement_accepted, onboarding_completed)
VALUES
  ('admin', 'admin@admin.com', '$2b$10$2O6WrAl3D4EdQI9s6ynC9uc4pX6.J8bJvr28s9aVmUs/P0m2LzneK', 'admin', 1, 1)
ON DUPLICATE KEY UPDATE
  username = 'admin',
  password_hash = VALUES(password_hash),
  role_type = 'admin',
  onboarding_completed = 1;

INSERT INTO users (id, username, password_hash, nickname, role, enabled)
SELECT id, 'admin', password_hash, 'admin', 'ADMIN', 1
FROM auth_users
WHERE email = 'admin@admin.com'
ON DUPLICATE KEY UPDATE
  username = 'admin',
  password_hash = VALUES(password_hash),
  nickname = 'admin',
  role = 'ADMIN',
  enabled = 1;

INSERT INTO profile_user_profiles
  (user_id, display_name, handle, role_label, bio, location, school)
SELECT id, 'admin', '@admin', '管理员', '系统管理员账号', 'China', 'StudyPlatform'
FROM auth_users
WHERE email = 'admin@admin.com'
ON DUPLICATE KEY UPDATE
  display_name = 'admin',
  handle = '@admin',
  role_label = '管理员',
  bio = '系统管理员账号',
  school = 'StudyPlatform';
