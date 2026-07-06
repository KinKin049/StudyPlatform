package com.cupk.rewards;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CoinRewardRepository {
    private final JdbcTemplate jdbcTemplate;

    public CoinRewardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean insertReward(
            long userId,
            String sourceType,
            String sourceKey,
            String reason,
            int amount,
            Long referenceId
    ) {
        if (amount <= 0) {
            return false;
        }
        String sql = """
                INSERT INTO coin_reward_records
                  (user_id, source_type, source_key, reason, amount, reference_id)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE id = id
                """;
        return jdbcTemplate.update(
                sql,
                userId,
                sourceType,
                sourceKey,
                reason,
                amount,
                referenceId
        ) > 0;
    }

    public long sumRewards(long userId) {
        Long value = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(amount), 0)
                FROM coin_reward_records
                WHERE user_id = ?
                """,
                Long.class,
                userId
        );
        return value == null ? 0L : value;
    }
}
