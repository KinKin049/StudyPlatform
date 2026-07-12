package com.cupk.rewards;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 金币奖励数据访问层，提供金币发放、消费和余额查询等功能。
 */
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
                SELECT COALESCE(r.reward_total, 0) - COALESCE(s.spend_total, 0)
                FROM (SELECT ? AS user_id) u
                LEFT JOIN (
                  SELECT user_id, SUM(amount) AS reward_total
                  FROM coin_reward_records
                  WHERE user_id = ?
                  GROUP BY user_id
                ) r ON r.user_id = u.user_id
                LEFT JOIN (
                  SELECT user_id, SUM(amount) AS spend_total
                  FROM coin_spend_records
                  WHERE user_id = ?
                  GROUP BY user_id
                ) s ON s.user_id = u.user_id
                """,
                Long.class,
                userId,
                userId,
                userId
        );
        return value == null ? 0L : value;
    }

    public boolean insertSpend(
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
                INSERT INTO coin_spend_records
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
}
