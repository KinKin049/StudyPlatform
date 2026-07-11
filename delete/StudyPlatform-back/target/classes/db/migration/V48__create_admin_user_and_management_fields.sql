SET @profile_admin_coin_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'profile_user_profiles'
    AND column_name = 'admin_coin_adjustment'
);

SET @profile_admin_coin_sql := IF(
  @profile_admin_coin_exists = 0,
  'ALTER TABLE profile_user_profiles ADD COLUMN admin_coin_adjustment BIGINT NOT NULL DEFAULT 0',
  'SELECT 1'
);

PREPARE profile_admin_coin_stmt FROM @profile_admin_coin_sql;
EXECUTE profile_admin_coin_stmt;
DEALLOCATE PREPARE profile_admin_coin_stmt;

SET @profile_admin_note_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'profile_user_profiles'
    AND column_name = 'admin_data_note'
);

SET @profile_admin_note_sql := IF(
  @profile_admin_note_exists = 0,
  'ALTER TABLE profile_user_profiles ADD COLUMN admin_data_note VARCHAR(512) NULL',
  'SELECT 1'
);

PREPARE profile_admin_note_stmt FROM @profile_admin_note_sql;
EXECUTE profile_admin_note_stmt;
DEALLOCATE PREPARE profile_admin_note_stmt;

SET @online_course_certified_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'online_open_courses'
    AND column_name = 'certified'
);

SET @online_course_certified_sql := IF(
  @online_course_certified_exists = 0,
  'ALTER TABLE online_open_courses ADD COLUMN certified TINYINT(1) NOT NULL DEFAULT 0',
  'SELECT 1'
);

PREPARE online_course_certified_stmt FROM @online_course_certified_sql;
EXECUTE online_course_certified_stmt;
DEALLOCATE PREPARE online_course_certified_stmt;

SET @general_course_certified_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'general_courses'
    AND column_name = 'certified'
);

SET @general_course_certified_sql := IF(
  @general_course_certified_exists = 0,
  'ALTER TABLE general_courses ADD COLUMN certified TINYINT(1) NOT NULL DEFAULT 0',
  'SELECT 1'
);

PREPARE general_course_certified_stmt FROM @general_course_certified_sql;
EXECUTE general_course_certified_stmt;
DEALLOCATE PREPARE general_course_certified_stmt;

SET @micro_major_certified_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'micro_major_courses'
    AND column_name = 'certified'
);

SET @micro_major_certified_sql := IF(
  @micro_major_certified_exists = 0,
  'ALTER TABLE micro_major_courses ADD COLUMN certified TINYINT(1) NOT NULL DEFAULT 0',
  'SELECT 1'
);

PREPARE micro_major_certified_stmt FROM @micro_major_certified_sql;
EXECUTE micro_major_certified_stmt;
DEALLOCATE PREPARE micro_major_certified_stmt;

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
