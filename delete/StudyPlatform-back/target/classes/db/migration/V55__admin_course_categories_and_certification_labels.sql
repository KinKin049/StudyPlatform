CREATE TABLE IF NOT EXISTS admin_course_categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_type VARCHAR(64) NOT NULL,
  name VARCHAR(80) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_admin_course_categories_resource_name (resource_type, name)
);

INSERT IGNORE INTO admin_course_categories (resource_type, name, sort_order)
SELECT 'online-open-courses', category, ROW_NUMBER() OVER (ORDER BY category)
FROM (
  SELECT DISTINCT category
  FROM online_open_courses
  WHERE category IS NOT NULL AND category <> ''
) categories;

INSERT IGNORE INTO admin_course_categories (resource_type, name, sort_order)
SELECT 'general-courses', category, ROW_NUMBER() OVER (ORDER BY category)
FROM (
  SELECT DISTINCT category
  FROM general_courses
  WHERE category IS NOT NULL AND category <> ''
) categories;

INSERT IGNORE INTO admin_course_categories (resource_type, name, sort_order)
SELECT 'micro-major-courses', category, ROW_NUMBER() OVER (ORDER BY category)
FROM (
  SELECT DISTINCT category
  FROM micro_major_courses
  WHERE category IS NOT NULL AND category <> ''
) categories;

SET @online_course_certification_label_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'online_open_courses'
    AND column_name = 'certification_label'
);

SET @online_course_certification_label_sql := IF(
  @online_course_certification_label_exists = 0,
  'ALTER TABLE online_open_courses ADD COLUMN certification_label VARCHAR(80) NULL',
  'SELECT 1'
);

PREPARE online_course_certification_label_stmt FROM @online_course_certification_label_sql;
EXECUTE online_course_certification_label_stmt;
DEALLOCATE PREPARE online_course_certification_label_stmt;

SET @general_course_certification_label_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'general_courses'
    AND column_name = 'certification_label'
);

SET @general_course_certification_label_sql := IF(
  @general_course_certification_label_exists = 0,
  'ALTER TABLE general_courses ADD COLUMN certification_label VARCHAR(80) NULL',
  'SELECT 1'
);

PREPARE general_course_certification_label_stmt FROM @general_course_certification_label_sql;
EXECUTE general_course_certification_label_stmt;
DEALLOCATE PREPARE general_course_certification_label_stmt;

SET @micro_major_certification_label_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'micro_major_courses'
    AND column_name = 'certification_label'
);

SET @micro_major_certification_label_sql := IF(
  @micro_major_certification_label_exists = 0,
  'ALTER TABLE micro_major_courses ADD COLUMN certification_label VARCHAR(80) NULL',
  'SELECT 1'
);

PREPARE micro_major_certification_label_stmt FROM @micro_major_certification_label_sql;
EXECUTE micro_major_certification_label_stmt;
DEALLOCATE PREPARE micro_major_certification_label_stmt;

UPDATE online_open_courses
SET certification_label = '已认证'
WHERE certified = 1 AND (certification_label IS NULL OR certification_label = '');

UPDATE general_courses
SET certification_label = '已认证'
WHERE certified = 1 AND (certification_label IS NULL OR certification_label = '');

UPDATE micro_major_courses
SET certification_label = '已认证'
WHERE certified = 1 AND (certification_label IS NULL OR certification_label = '');
