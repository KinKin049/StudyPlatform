SET @course_review_teacher_read_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'academy_course_reviews'
    AND column_name = 'teacher_read_at'
);
SET @course_review_teacher_read_sql := IF(
  @course_review_teacher_read_exists = 0,
  'ALTER TABLE academy_course_reviews ADD COLUMN teacher_read_at DATETIME NULL AFTER replied_at',
  'SELECT 1'
);
PREPARE course_review_teacher_read_stmt FROM @course_review_teacher_read_sql;
EXECUTE course_review_teacher_read_stmt;
DEALLOCATE PREPARE course_review_teacher_read_stmt;

SET @course_review_teacher_read_index_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'academy_course_reviews'
    AND index_name = 'idx_academy_course_reviews_teacher_read'
);
SET @course_review_teacher_read_index_sql := IF(
  @course_review_teacher_read_index_exists = 0,
  'CREATE INDEX idx_academy_course_reviews_teacher_read ON academy_course_reviews (resource_type, course_id, teacher_read_at, created_at)',
  'SELECT 1'
);
PREPARE course_review_teacher_read_index_stmt FROM @course_review_teacher_read_index_sql;
EXECUTE course_review_teacher_read_index_stmt;
DEALLOCATE PREPARE course_review_teacher_read_index_stmt;
