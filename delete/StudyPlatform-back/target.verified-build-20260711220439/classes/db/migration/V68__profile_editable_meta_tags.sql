SET @profile_meta_tags_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'profile_user_profiles'
    AND column_name = 'profile_tags_json'
);
SET @profile_meta_tags_sql := IF(
  @profile_meta_tags_exists = 0,
  'ALTER TABLE profile_user_profiles ADD COLUMN profile_tags_json JSON NULL AFTER location',
  'SELECT 1'
);
PREPARE profile_meta_tags_stmt FROM @profile_meta_tags_sql;
EXECUTE profile_meta_tags_stmt;
DEALLOCATE PREPARE profile_meta_tags_stmt;

UPDATE profile_user_profiles p
LEFT JOIN users u ON u.id = p.user_id
SET p.profile_tags_json = JSON_ARRAY(
  CASE
    WHEN u.learning_goal IS NOT NULL AND u.learning_goal <> ''
      THEN CONCAT('目标：', u.learning_goal)
    ELSE '目标：稳稳变强'
  END
)
WHERE p.profile_tags_json IS NULL;
