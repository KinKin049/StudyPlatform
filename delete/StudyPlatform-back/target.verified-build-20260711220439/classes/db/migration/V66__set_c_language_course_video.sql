INSERT INTO teacher_published_courses
  (course_id, publisher_user_id, semester_plan, course_overview, course_detail, video_file_path)
SELECT
  '46004_1476538444',
  COALESCE(
    (SELECT id FROM users WHERE username = 'admin' LIMIT 1),
    (SELECT id FROM users ORDER BY id ASC LIMIT 1)
  ),
  NULL,
  NULL,
  NULL,
  'online_course/videos/c-language-programming-20260709-recording.mp4'
WHERE EXISTS (SELECT 1 FROM users LIMIT 1)
ON DUPLICATE KEY UPDATE
  video_file_path = VALUES(video_file_path),
  updated_at = CURRENT_TIMESTAMP;
