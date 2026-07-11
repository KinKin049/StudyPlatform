CREATE TABLE IF NOT EXISTS academy_textbook_payments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_id VARCHAR(96) NOT NULL,
  order_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL DEFAULT 1,
  provider VARCHAR(32) NOT NULL,
  amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  payment_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  qr_payload VARCHAR(512) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at DATETIME NOT NULL,
  paid_at DATETIME NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_academy_textbook_payments_session (session_id),
  KEY idx_academy_textbook_payments_order (order_no),
  KEY idx_academy_textbook_payments_user (user_id),
  CONSTRAINT fk_academy_textbook_payments_order
    FOREIGN KEY (order_no) REFERENCES academy_textbook_orders (order_no) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
