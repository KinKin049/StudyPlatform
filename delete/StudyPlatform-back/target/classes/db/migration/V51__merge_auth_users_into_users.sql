SET @users_email_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND column_name = 'email'
);
SET @users_email_sql := IF(
  @users_email_exists = 0,
  'ALTER TABLE users ADD COLUMN email VARCHAR(128) NULL AFTER avatar_url',
  'SELECT 1'
);
PREPARE users_email_stmt FROM @users_email_sql;
EXECUTE users_email_stmt;
DEALLOCATE PREPARE users_email_stmt;

SET @users_role_type_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND column_name = 'role_type'
);
SET @users_role_type_sql := IF(
  @users_role_type_exists = 0,
  'ALTER TABLE users ADD COLUMN role_type VARCHAR(24) NULL AFTER email',
  'SELECT 1'
);
PREPARE users_role_type_stmt FROM @users_role_type_sql;
EXECUTE users_role_type_stmt;
DEALLOCATE PREPARE users_role_type_stmt;

SET @users_learning_goal_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND column_name = 'learning_goal'
);
SET @users_learning_goal_sql := IF(
  @users_learning_goal_exists = 0,
  'ALTER TABLE users ADD COLUMN learning_goal VARCHAR(255) NULL AFTER role_type',
  'SELECT 1'
);
PREPARE users_learning_goal_stmt FROM @users_learning_goal_sql;
EXECUTE users_learning_goal_stmt;
DEALLOCATE PREPARE users_learning_goal_stmt;

SET @users_interests_json_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND column_name = 'interests_json'
);
SET @users_interests_json_sql := IF(
  @users_interests_json_exists = 0,
  'ALTER TABLE users ADD COLUMN interests_json JSON NULL AFTER learning_goal',
  'SELECT 1'
);
PREPARE users_interests_json_stmt FROM @users_interests_json_sql;
EXECUTE users_interests_json_stmt;
DEALLOCATE PREPARE users_interests_json_stmt;

SET @users_school_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND column_name = 'school'
);
SET @users_school_sql := IF(
  @users_school_exists = 0,
  'ALTER TABLE users ADD COLUMN school VARCHAR(128) NULL AFTER interests_json',
  'SELECT 1'
);
PREPARE users_school_stmt FROM @users_school_sql;
EXECUTE users_school_stmt;
DEALLOCATE PREPARE users_school_stmt;

SET @users_teacher_name_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND column_name = 'teacher_name'
);
SET @users_teacher_name_sql := IF(
  @users_teacher_name_exists = 0,
  'ALTER TABLE users ADD COLUMN teacher_name VARCHAR(64) NULL AFTER school',
  'SELECT 1'
);
PREPARE users_teacher_name_stmt FROM @users_teacher_name_sql;
EXECUTE users_teacher_name_stmt;
DEALLOCATE PREPARE users_teacher_name_stmt;

SET @users_pet_key_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND column_name = 'pet_key'
);
SET @users_pet_key_sql := IF(
  @users_pet_key_exists = 0,
  'ALTER TABLE users ADD COLUMN pet_key VARCHAR(32) NULL AFTER teacher_name',
  'SELECT 1'
);
PREPARE users_pet_key_stmt FROM @users_pet_key_sql;
EXECUTE users_pet_key_stmt;
DEALLOCATE PREPARE users_pet_key_stmt;

SET @users_agreement_accepted_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND column_name = 'agreement_accepted'
);
SET @users_agreement_accepted_sql := IF(
  @users_agreement_accepted_exists = 0,
  'ALTER TABLE users ADD COLUMN agreement_accepted TINYINT(1) NOT NULL DEFAULT 0 AFTER pet_key',
  'SELECT 1'
);
PREPARE users_agreement_accepted_stmt FROM @users_agreement_accepted_sql;
EXECUTE users_agreement_accepted_stmt;
DEALLOCATE PREPARE users_agreement_accepted_stmt;

SET @users_onboarding_completed_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND column_name = 'onboarding_completed'
);
SET @users_onboarding_completed_sql := IF(
  @users_onboarding_completed_exists = 0,
  'ALTER TABLE users ADD COLUMN onboarding_completed TINYINT(1) NOT NULL DEFAULT 0 AFTER agreement_accepted',
  'SELECT 1'
);
PREPARE users_onboarding_completed_stmt FROM @users_onboarding_completed_sql;
EXECUTE users_onboarding_completed_stmt;
DEALLOCATE PREPARE users_onboarding_completed_stmt;

INSERT IGNORE INTO users (id, username, password_hash, nickname, role, enabled)
SELECT a.id,
       CONCAT(a.username, '_', a.id),
       a.password_hash,
       a.username,
       CASE
         WHEN a.role_type = 'teacher' THEN 'TEACHER'
         WHEN a.role_type = 'admin' THEN 'ADMIN'
         ELSE 'STUDENT'
       END,
       1
FROM auth_users a;

UPDATE users u
JOIN auth_users a ON a.id = u.id
SET u.email = a.email,
    u.password_hash = a.password_hash,
    u.nickname = COALESCE(NULLIF(a.username, ''), u.nickname, u.username),
    u.role_type = COALESCE(NULLIF(a.role_type, ''),
      CASE
        WHEN u.role = 'TEACHER' THEN 'teacher'
        WHEN u.role = 'ADMIN' THEN 'admin'
        ELSE 'student'
      END
    ),
    u.role = CASE
      WHEN a.role_type = 'teacher' THEN 'TEACHER'
      WHEN a.role_type = 'admin' THEN 'ADMIN'
      ELSE 'STUDENT'
    END,
    u.learning_goal = a.learning_goal,
    u.interests_json = a.interests_json,
    u.school = a.school,
    u.teacher_name = a.teacher_name,
    u.pet_key = a.pet_key,
    u.agreement_accepted = COALESCE(a.agreement_accepted, 0),
    u.onboarding_completed = COALESCE(a.onboarding_completed, 0);

UPDATE users
SET email = CONCAT('legacy-user-', id, '@local.study')
WHERE email IS NULL OR email = '';

UPDATE users
SET role_type = CASE
  WHEN role = 'TEACHER' THEN 'teacher'
  WHEN role = 'ADMIN' THEN 'admin'
  ELSE 'student'
END
WHERE role_type IS NULL OR role_type = '';

UPDATE users
SET nickname = username
WHERE nickname IS NULL OR nickname = '';

SET @users_email_index_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND index_name = 'uk_users_email'
);
SET @users_email_index_sql := IF(
  @users_email_index_exists = 0,
  'ALTER TABLE users ADD UNIQUE KEY uk_users_email (email)',
  'SELECT 1'
);
PREPARE users_email_index_stmt FROM @users_email_index_sql;
EXECUTE users_email_index_stmt;
DEALLOCATE PREPARE users_email_index_stmt;

DROP TABLE auth_users;

CREATE VIEW auth_users AS
SELECT id,
       COALESCE(NULLIF(nickname, ''), username) AS username,
       email,
       password_hash,
       COALESCE(NULLIF(role_type, ''),
         CASE
           WHEN role = 'TEACHER' THEN 'teacher'
           WHEN role = 'ADMIN' THEN 'admin'
           ELSE 'student'
         END
       ) AS role_type,
       learning_goal,
       interests_json,
       school,
       teacher_name,
       pet_key,
       agreement_accepted,
       onboarding_completed,
       created_at,
       updated_at
FROM users;
