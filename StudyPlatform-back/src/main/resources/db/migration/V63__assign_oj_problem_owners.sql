UPDATE oj_problems p
JOIN (
  SELECT id
  FROM users
  WHERE email = 'admin@admin.com'
  ORDER BY id ASC
  LIMIT 1
) admin_user ON p.created_by IS NULL
SET p.created_by = admin_user.id;

CREATE INDEX idx_oj_problems_created_by ON oj_problems (created_by);
