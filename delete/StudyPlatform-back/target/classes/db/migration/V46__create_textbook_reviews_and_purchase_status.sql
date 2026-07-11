CREATE TABLE IF NOT EXISTS academy_textbook_reviews (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL DEFAULT 1,
  textbook_id VARCHAR(128) NOT NULL,
  user_name VARCHAR(80) NOT NULL DEFAULT '默认用户',
  rating INT NOT NULL DEFAULT 5,
  content TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_academy_textbook_reviews_textbook (textbook_id),
  KEY idx_academy_textbook_reviews_user_textbook (user_id, textbook_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
