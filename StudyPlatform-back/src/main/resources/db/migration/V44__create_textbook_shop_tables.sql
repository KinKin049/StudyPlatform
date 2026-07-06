CREATE TABLE IF NOT EXISTS academy_textbook_details (
  id BIGINT NOT NULL AUTO_INCREMENT,
  textbook_id VARCHAR(128) NOT NULL,
  recommendation TEXT NULL,
  original_price DECIMAL(10,2) NOT NULL DEFAULT 69.00,
  discount_price DECIMAL(10,2) NOT NULL DEFAULT 49.00,
  reader_count INT NOT NULL DEFAULT 0,
  overview TEXT NULL,
  catalog_text TEXT NULL,
  comments_text TEXT NULL,
  crawled_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_academy_textbook_details_textbook (textbook_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS academy_textbook_cart_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL DEFAULT 1,
  textbook_id VARCHAR(128) NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_academy_textbook_cart_user_textbook (user_id, textbook_id),
  KEY idx_academy_textbook_cart_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS academy_textbook_orders (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL DEFAULT 1,
  order_no VARCHAR(64) NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  order_status VARCHAR(32) NOT NULL DEFAULT '待支付',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_academy_textbook_orders_no (order_no),
  KEY idx_academy_textbook_orders_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS academy_textbook_order_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  textbook_id VARCHAR(128) NOT NULL,
  textbook_name VARCHAR(255) NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  quantity INT NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_academy_textbook_order_items_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO academy_textbook_details
  (textbook_id, recommendation, original_price, discount_price, reader_count, overview, catalog_text, comments_text, crawled_at)
SELECT
  external_textbook_id,
  description,
  ROUND(58.00 + (id MOD 9) * 6.00 + 0.80, 2),
  ROUND((58.00 + (id MOD 9) * 6.00 + 0.80) * 0.78, 2),
  1200 + id * 37,
  CONCAT(
    textbook_name,
    '围绕“',
    COALESCE(category, '通识学习'),
    '”方向组织内容，由',
    COALESCE(publisher, '出版社'),
    '出版。教材信息、推荐语与封面来自现有精品教材数据源；目录、评论和价格为购物模块展示准备的结构化数据，可在后续爬虫或人工维护时逐步替换为真实字段。'
  ),
  CONCAT(
    '导学：教材特色与学习路径', CHAR(10),
    '第一章：核心概念与基础知识', CHAR(10),
    '第二章：重点内容解析与案例', CHAR(10),
    '第三章：实践任务与拓展阅读', CHAR(10),
    '复习：综合练习与学习评价'
  ),
  CONCAT(
    '学习委员|5|内容结构清晰，适合作为课程学习配套教材。', CHAR(10),
    '平台用户|4|数字资源比较丰富，概览和目录对选书很有帮助。', CHAR(10),
    '教师推荐|5|知识覆盖完整，适合作为课堂教学参考。'
  ),
  CURRENT_TIMESTAMP
FROM excellent_textbooks
ON DUPLICATE KEY UPDATE
  recommendation = VALUES(recommendation),
  original_price = VALUES(original_price),
  discount_price = VALUES(discount_price),
  reader_count = VALUES(reader_count),
  overview = VALUES(overview),
  catalog_text = VALUES(catalog_text),
  comments_text = VALUES(comments_text),
  crawled_at = VALUES(crawled_at);
