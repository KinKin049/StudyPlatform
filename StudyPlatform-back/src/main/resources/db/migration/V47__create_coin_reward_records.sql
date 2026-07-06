CREATE TABLE IF NOT EXISTS coin_reward_records (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  source_key VARCHAR(128) NOT NULL,
  reason VARCHAR(255) NOT NULL,
  amount INT NOT NULL DEFAULT 0,
  reference_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_coin_reward_source (user_id, source_type, source_key),
  KEY idx_coin_reward_user_created (user_id, created_at),
  KEY idx_coin_reward_user_source (user_id, source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO coin_reward_records
  (user_id, source_type, source_key, reason, amount, reference_id, created_at)
SELECT user_id,
       'learning_time',
       CONCAT('learning-time:', id),
       CASE module_type
         WHEN 'video' THEN '在线课程学习时长'
         WHEN 'visualization' THEN '可视化学习在线时长'
         WHEN 'petroleum' THEN '实验平台在线时长'
         WHEN 'oj' THEN 'OJ 平台练习时长'
         WHEN 'question_bank' THEN '题库练习时长'
         WHEN 'question' THEN '题库练习时长'
         WHEN 'mistake' THEN '错题本复习时长'
         WHEN 'favorite' THEN '收藏题目复习时长'
         WHEN 'assignment' THEN '作业练习时长'
         WHEN 'exam' THEN '考试练习时长'
         ELSE '学习在线时长'
       END,
       FLOOR(duration_seconds / 600) *
       CASE module_type
         WHEN 'video' THEN 5
         WHEN 'visualization' THEN 3
         WHEN 'petroleum' THEN 4
         WHEN 'oj' THEN 2
         WHEN 'question_bank' THEN 2
         WHEN 'question' THEN 2
         WHEN 'mistake' THEN 2
         WHEN 'favorite' THEN 2
         WHEN 'assignment' THEN 2
         WHEN 'exam' THEN 2
         ELSE 1
       END,
       id,
       created_at
FROM profile_learning_time_records
WHERE duration_seconds >= 600;

INSERT IGNORE INTO coin_reward_records
  (user_id, source_type, source_key, reason, amount, reference_id, created_at)
SELECT user_id,
       'learning_event',
       CONCAT('learning-event:', id),
       CASE
         WHEN event_type = 'vocabulary' THEN '单词掌握奖励'
         ELSE '题目答对奖励'
       END,
       CASE
         WHEN event_type = 'answer' AND is_correct = 1 AND question_type IN ('multiple', 'short') THEN 2
         WHEN event_type = 'answer' AND is_correct = 1 THEN 1
         WHEN event_type = 'vocabulary' AND vocabulary_status = 'known' THEN 1
         ELSE 0
       END,
       id,
       created_at
FROM profile_learning_events
WHERE (event_type = 'answer' AND is_correct = 1)
   OR (event_type = 'vocabulary' AND vocabulary_status = 'known');

INSERT IGNORE INTO coin_reward_records
  (user_id, source_type, source_key, reason, amount, reference_id, created_at)
SELECT user_id,
       'game',
       CONCAT('ladder-jump:', id),
       '万题天梯跳游戏获得',
       total_coins,
       id,
       created_at
FROM game_ladder_jump_records
WHERE total_coins > 0;

INSERT IGNORE INTO coin_reward_records
  (user_id, source_type, source_key, reason, amount, reference_id, created_at)
SELECT user_id,
       'game',
       CONCAT('type-warrior:', id),
       'Type Warrior 分数兑换',
       ROUND(score / 100),
       id,
       created_at
FROM game_type_warrior_records
WHERE ROUND(score / 100) > 0;
