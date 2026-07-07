ALTER TABLE academy_textbook_orders
  ADD COLUMN original_amount DECIMAL(10, 2) NULL AFTER total_amount,
  ADD COLUMN discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00 AFTER original_amount,
  ADD COLUMN voucher_key VARCHAR(64) NULL AFTER discount_amount,
  ADD COLUMN voucher_name VARCHAR(128) NULL AFTER voucher_key,
  ADD COLUMN voucher_consumed TINYINT(1) NOT NULL DEFAULT 0 AFTER voucher_name;

UPDATE academy_textbook_orders
SET original_amount = total_amount
WHERE original_amount IS NULL;

ALTER TABLE academy_textbook_orders
  MODIFY COLUMN original_amount DECIMAL(10, 2) NOT NULL;
