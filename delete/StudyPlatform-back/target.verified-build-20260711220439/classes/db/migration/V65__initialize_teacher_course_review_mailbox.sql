UPDATE academy_course_reviews
SET teacher_read_at = COALESCE(teacher_read_at, CURRENT_TIMESTAMP)
WHERE teacher_read_at IS NULL;
