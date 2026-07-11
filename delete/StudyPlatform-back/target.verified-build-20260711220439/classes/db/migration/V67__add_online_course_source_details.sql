SET @online_course_description_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'online_open_courses'
    AND column_name = 'course_description'
);

SET @online_course_description_sql := IF(
  @online_course_description_exists = 0,
  'ALTER TABLE online_open_courses ADD COLUMN course_description TEXT NULL AFTER course_comment',
  'SELECT 1'
);

PREPARE online_course_description_stmt FROM @online_course_description_sql;
EXECUTE online_course_description_stmt;
DEALLOCATE PREPARE online_course_description_stmt;

SET @online_course_source_synced_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'online_open_courses'
    AND column_name = 'source_synced_at'
);

SET @online_course_source_synced_sql := IF(
  @online_course_source_synced_exists = 0,
  'ALTER TABLE online_open_courses ADD COLUMN source_synced_at DATETIME NULL AFTER source_url',
  'SELECT 1'
);

PREPARE online_course_source_synced_stmt FROM @online_course_source_synced_sql;
EXECUTE online_course_source_synced_stmt;
DEALLOCATE PREPARE online_course_source_synced_stmt;
