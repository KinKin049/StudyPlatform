SET @course_review_user_id_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'academy_course_reviews'
    AND column_name = 'user_id'
);
SET @course_review_user_id_sql := IF(
  @course_review_user_id_exists = 0,
  'ALTER TABLE academy_course_reviews ADD COLUMN user_id BIGINT NULL AFTER course_id',
  'SELECT 1'
);
PREPARE course_review_user_id_stmt FROM @course_review_user_id_sql;
EXECUTE course_review_user_id_stmt;
DEALLOCATE PREPARE course_review_user_id_stmt;

SET @course_review_reply_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'academy_course_reviews'
    AND column_name = 'reply_content'
);
SET @course_review_reply_sql := IF(
  @course_review_reply_exists = 0,
  'ALTER TABLE academy_course_reviews ADD COLUMN reply_content TEXT NULL, ADD COLUMN reply_user_id BIGINT NULL, ADD COLUMN reply_user_name VARCHAR(80) NULL, ADD COLUMN reply_user_role_type VARCHAR(24) NULL, ADD COLUMN replied_at DATETIME NULL',
  'SELECT 1'
);
PREPARE course_review_reply_stmt FROM @course_review_reply_sql;
EXECUTE course_review_reply_stmt;
DEALLOCATE PREPARE course_review_reply_stmt;

SET @textbook_review_reply_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'academy_textbook_reviews'
    AND column_name = 'reply_content'
);
SET @textbook_review_reply_sql := IF(
  @textbook_review_reply_exists = 0,
  'ALTER TABLE academy_textbook_reviews ADD COLUMN reply_content TEXT NULL, ADD COLUMN reply_user_id BIGINT NULL, ADD COLUMN reply_user_name VARCHAR(80) NULL, ADD COLUMN reply_user_role_type VARCHAR(24) NULL, ADD COLUMN replied_at DATETIME NULL',
  'SELECT 1'
);
PREPARE textbook_review_reply_stmt FROM @textbook_review_reply_sql;
EXECUTE textbook_review_reply_stmt;
DEALLOCATE PREPARE textbook_review_reply_stmt;
