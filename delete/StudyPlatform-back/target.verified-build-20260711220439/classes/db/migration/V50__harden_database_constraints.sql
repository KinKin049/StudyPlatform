INSERT INTO auth_users
  (id, username, email, password_hash, role_type, agreement_accepted, onboarding_completed)
SELECT u.id,
       u.nickname,
       CONCAT('legacy-user-', u.id, '@local.study'),
       u.password_hash,
       CASE
         WHEN u.role = 'TEACHER' THEN 'teacher'
         WHEN u.role = 'ADMIN' THEN 'admin'
         ELSE 'student'
       END,
       1,
       1
FROM users u
LEFT JOIN auth_users a ON a.id = u.id
WHERE a.id IS NULL;

DROP TABLE IF EXISTS flyway_schema_history_backup_20260702_161139;

ALTER TABLE profile_user_profiles
  ADD CONSTRAINT fk_profile_user_profiles_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE profile_learning_events
  ADD CONSTRAINT fk_profile_learning_events_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE profile_learning_time_records
  ADD CONSTRAINT fk_profile_learning_time_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE course_question_bank_mistakes
  ADD CONSTRAINT fk_course_question_bank_mistakes_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE course_question_bank_favorites
  ADD CONSTRAINT fk_course_question_bank_favorites_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE game_ladder_jump_records
  ADD CONSTRAINT fk_game_ladder_jump_records_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE game_type_warrior_records
  ADD CONSTRAINT fk_game_type_warrior_records_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE coin_reward_records
  ADD CONSTRAINT fk_coin_reward_records_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE academy_textbook_cart_items
  ADD CONSTRAINT fk_academy_textbook_cart_items_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE academy_textbook_orders
  ADD CONSTRAINT fk_academy_textbook_orders_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE academy_textbook_order_items
  ADD CONSTRAINT fk_academy_textbook_order_items_order
    FOREIGN KEY (order_id) REFERENCES academy_textbook_orders (id) ON DELETE CASCADE;

ALTER TABLE academy_textbook_reviews
  ADD CONSTRAINT fk_academy_textbook_reviews_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE teacher_published_courses
  ADD CONSTRAINT fk_teacher_published_courses_user
    FOREIGN KEY (publisher_user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE academy_textbook_cart_items
  ADD CONSTRAINT chk_academy_textbook_cart_quantity CHECK (quantity > 0);

ALTER TABLE academy_textbook_order_items
  ADD CONSTRAINT chk_academy_textbook_order_items_quantity CHECK (quantity > 0),
  ADD CONSTRAINT chk_academy_textbook_order_items_price CHECK (unit_price >= 0);

ALTER TABLE academy_textbook_reviews
  ADD CONSTRAINT chk_academy_textbook_reviews_rating CHECK (rating BETWEEN 1 AND 5);

ALTER TABLE coin_reward_records
  ADD CONSTRAINT chk_coin_reward_records_amount CHECK (amount >= 0);
