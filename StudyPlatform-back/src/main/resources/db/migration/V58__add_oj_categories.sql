CREATE TABLE IF NOT EXISTS oj_categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_oj_categories_name (name)
);

SET @oj_problem_category_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'oj_problems'
    AND column_name = 'category'
);

SET @oj_problem_category_sql := IF(
  @oj_problem_category_exists = 0,
  'ALTER TABLE oj_problems ADD COLUMN category VARCHAR(80) NULL AFTER slug',
  'SELECT 1'
);

PREPARE oj_problem_category_stmt FROM @oj_problem_category_sql;
EXECUTE oj_problem_category_stmt;
DEALLOCATE PREPARE oj_problem_category_stmt;

INSERT IGNORE INTO oj_categories (name, sort_order)
VALUES ('默认分类', 1), ('基础语法', 2), ('数据结构', 3), ('算法练习', 4);

UPDATE oj_problems
SET category = '默认分类'
WHERE category IS NULL OR category = '';

INSERT IGNORE INTO oj_categories (name, sort_order)
SELECT category, ROW_NUMBER() OVER (ORDER BY category) + 10
FROM (
  SELECT DISTINCT category
  FROM oj_problems
  WHERE category IS NOT NULL AND category <> ''
) categories;
