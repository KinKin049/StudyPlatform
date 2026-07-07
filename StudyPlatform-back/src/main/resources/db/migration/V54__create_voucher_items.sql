CREATE TABLE IF NOT EXISTS voucher_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  voucher_key VARCHAR(64) NOT NULL,
  voucher_type VARCHAR(32) NOT NULL,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(255) NOT NULL,
  price INT NOT NULL DEFAULT 0,
  stock_quantity INT NULL,
  unlimited_stock TINYINT(1) NOT NULL DEFAULT 1,
  discount_type VARCHAR(24) NOT NULL DEFAULT 'NONE',
  threshold_amount DECIMAL(10, 2) NULL,
  discount_amount DECIMAL(10, 2) NULL,
  discount_rate DECIMAL(6, 4) NULL,
  max_discount_amount DECIMAL(10, 2) NULL,
  valid_from DATETIME NULL,
  valid_until DATETIME NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_voucher_items_key (voucher_key),
  KEY idx_voucher_items_enabled_type (enabled, voucher_type, sort_order),
  CONSTRAINT chk_voucher_items_price CHECK (price >= 0),
  CONSTRAINT chk_voucher_items_stock CHECK (stock_quantity IS NULL OR stock_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO voucher_items
  (voucher_key, voucher_type, name, description, price, stock_quantity, unlimited_stock,
   discount_type, threshold_amount, discount_amount, discount_rate, max_discount_amount,
   valid_from, valid_until, enabled, sort_order)
VALUES
  ('type-warrior-skill-refresh', 'GAME_ITEM', 'Type Warrior 技能刷新券', '技能选择弹窗出现时可刷新一次候选技能。', 260, NULL, 1,
   'NONE', NULL, NULL, NULL, NULL, NULL, NULL, 1, 10),
  ('game-revive', 'GAME_ITEM', '游戏复活券', '在 Type Warrior 或万题天梯跳失败后可立即复活一次。', 360, NULL, 1,
   'NONE', NULL, NULL, NULL, NULL, NULL, NULL, 1, 20),
  ('coupon-course-30-5', 'DISCOUNT', '满 30 元减 5 元优惠券', '课程资料与学习权益展示券，后续可接入真实抵扣。', 300, NULL, 1,
   'AMOUNT', 30.00, 5.00, NULL, 5.00, NULL, NULL, 1, 100),
  ('coupon-textbook-80-15', 'DISCOUNT', '满 80 元减 15 元优惠券', '教材购买可用，支付成功后扣减库存。', 700, NULL, 1,
   'AMOUNT', 80.00, 15.00, NULL, 15.00, NULL, NULL, 1, 110),
  ('coupon-study-90', 'DISCOUNT', '课程资料 9 折券', '课程资料折扣展示券，后续可接入真实抵扣。', 500, NULL, 1,
   'PERCENT', 0.00, NULL, 0.9000, NULL, NULL, NULL, 1, 120)
ON DUPLICATE KEY UPDATE
  voucher_type = VALUES(voucher_type),
  name = VALUES(name),
  description = VALUES(description);
