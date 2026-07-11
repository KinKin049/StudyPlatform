SET @auth_username_index_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'auth_users'
    AND index_name = 'uk_auth_users_username'
);

SET @drop_auth_username_index_sql := IF(
  @auth_username_index_exists > 0,
  'ALTER TABLE auth_users DROP INDEX uk_auth_users_username',
  'SELECT 1'
);

PREPARE drop_auth_username_index_stmt FROM @drop_auth_username_index_sql;
EXECUTE drop_auth_username_index_stmt;
DEALLOCATE PREPARE drop_auth_username_index_stmt;
