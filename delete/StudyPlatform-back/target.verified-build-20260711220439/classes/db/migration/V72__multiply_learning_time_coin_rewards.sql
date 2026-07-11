UPDATE coin_reward_records
SET amount = amount * 10
WHERE source_type = 'learning_time'
  AND amount > 0;
