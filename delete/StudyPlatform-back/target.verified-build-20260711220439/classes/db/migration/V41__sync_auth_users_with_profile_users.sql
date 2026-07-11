INSERT IGNORE INTO users (id, username, password_hash, nickname, role, enabled)
SELECT id,
       username,
       password_hash,
       username,
       CASE WHEN role_type = 'teacher' THEN 'TEACHER' ELSE 'STUDENT' END,
       1
FROM auth_users;

INSERT INTO profile_user_profiles
  (user_id, display_name, handle, role_label, bio, location, school)
SELECT id,
       username,
       CONCAT('@', username),
       CASE WHEN role_type = 'teacher' THEN '教师' ELSE '学生' END,
       CASE
         WHEN role_type = 'teacher' AND teacher_name IS NOT NULL AND teacher_name <> ''
           THEN CONCAT('教师：', teacher_name)
         WHEN learning_goal IS NOT NULL AND learning_goal <> ''
           THEN CONCAT('目标：', learning_goal)
         ELSE '这个账号正在完善自己的学习主页。'
       END,
       'China',
       COALESCE(NULLIF(school, ''), 'StudyPlatform')
FROM auth_users
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  handle = VALUES(handle),
  role_label = VALUES(role_label),
  bio = VALUES(bio),
  school = VALUES(school);
