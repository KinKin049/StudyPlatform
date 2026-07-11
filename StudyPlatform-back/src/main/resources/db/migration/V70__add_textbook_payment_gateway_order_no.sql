ALTER TABLE academy_textbook_payments
  ADD COLUMN gateway_order_no VARCHAR(96) NULL AFTER order_no,
  ADD KEY idx_academy_textbook_payments_gateway_order (gateway_order_no);

UPDATE academy_textbook_payments
SET gateway_order_no = order_no
WHERE gateway_order_no IS NULL OR gateway_order_no = '';
