SET @course_review_parent_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'academy_course_reviews'
    AND column_name = 'parent_review_id'
);
SET @course_review_parent_sql := IF(
  @course_review_parent_exists = 0,
  'ALTER TABLE academy_course_reviews ADD COLUMN parent_review_id BIGINT NULL AFTER user_name',
  'SELECT 1'
);
PREPARE course_review_parent_stmt FROM @course_review_parent_sql;
EXECUTE course_review_parent_stmt;
DEALLOCATE PREPARE course_review_parent_stmt;

SET @course_review_parent_index_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'academy_course_reviews'
    AND index_name = 'idx_academy_course_reviews_parent'
);
SET @course_review_parent_index_sql := IF(
  @course_review_parent_index_exists = 0,
  'CREATE INDEX idx_academy_course_reviews_parent ON academy_course_reviews (parent_review_id, created_at)',
  'SELECT 1'
);
PREPARE course_review_parent_index_stmt FROM @course_review_parent_index_sql;
EXECUTE course_review_parent_index_stmt;
DEALLOCATE PREPARE course_review_parent_index_stmt;
