CREATE TABLE IF NOT EXISTS oj_categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_oj_categories_name (name)
);

INSERT IGNORE INTO oj_categories (name, sort_order)
VALUES
  ('beginner', 1),
  ('math', 2),
  ('number-theory', 3),
  ('array', 4),
  ('string', 5),
  ('stack', 6),
  ('hash-table', 7),
  ('sort', 8),
  ('interval', 9),
  ('dp', 10),
  ('binary-search', 11),
  ('graph', 12),
  ('bfs', 13),
  ('grid', 14),
  ('sieve', 15),
  ('prefix', 16);

DELETE FROM oj_categories
WHERE name NOT IN (
  'beginner',
  'math',
  'number-theory',
  'array',
  'string',
  'stack',
  'hash-table',
  'sort',
  'interval',
  'dp',
  'binary-search',
  'graph',
  'bfs',
  'grid',
  'sieve',
  'prefix'
);

UPDATE oj_problems
SET category = JSON_UNQUOTE(JSON_EXTRACT(tags, '$[0]'))
WHERE tags IS NOT NULL
  AND JSON_LENGTH(tags) > 0
  AND JSON_UNQUOTE(JSON_EXTRACT(tags, '$[0]')) IN (
    'beginner',
    'math',
    'number-theory',
    'array',
    'string',
    'stack',
    'hash-table',
    'sort',
    'interval',
    'dp',
    'binary-search',
    'graph',
    'bfs',
    'grid',
    'sieve',
    'prefix'
  );

UPDATE oj_problems
SET category = 'beginner'
WHERE category IS NULL
   OR category = ''
   OR category NOT IN (
    'beginner',
    'math',
    'number-theory',
    'array',
    'string',
    'stack',
    'hash-table',
    'sort',
    'interval',
    'dp',
    'binary-search',
    'graph',
    'bfs',
    'grid',
    'sieve',
    'prefix'
  );

UPDATE oj_problems
SET tags = JSON_ARRAY(category)
WHERE tags IS NULL OR JSON_LENGTH(tags) = 0;

UPDATE oj_problems
SET tags = JSON_ARRAY_APPEND(tags, '$', category)
WHERE category IS NOT NULL
  AND category <> ''
  AND JSON_CONTAINS(tags, JSON_QUOTE(category)) = 0;
