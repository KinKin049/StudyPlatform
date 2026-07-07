package com.cupk.games.repository;

import com.cupk.games.dto.LadderJumpRecordSaveRequest;
import com.cupk.games.dto.TypeWarriorRecordSaveRequest;
import java.sql.Statement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 游戏记录数据访问层，存储每次游戏运行记录并提供个人主页的聚合视图。
 */
@Repository
public class GameRecordRepository {
    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造函数
     *
     * @param jdbcTemplate JDBC模板
     */
    public GameRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 插入阶梯跳跃游戏记录
     *
     * @param userId 用户ID
     * @param request 记录保存请求
     * @return 记录ID
     */
    public long insertLadderJumpRecord(long userId, LadderJumpRecordSaveRequest request) {
        String sql = """
                INSERT INTO game_ladder_jump_records
                  (user_id, question_bank_code, total_coins, correct_count, wrong_count, duration_seconds)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setString(2, blankToNull(request.questionBankCode()));
            ps.setInt(3, safeInt(request.totalCoins()));
            ps.setInt(4, safeInt(request.correctCount()));
            ps.setInt(5, safeInt(request.wrongCount()));
            ps.setDouble(6, safeDouble(request.durationSeconds()));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    /**
     * 插入打字勇士游戏记录
     *
     * @param userId 用户ID
     * @param request 记录保存请求
     * @return 记录ID
     */
    public long insertTypeWarriorRecord(long userId, TypeWarriorRecordSaveRequest request) {
        String sql = """
                INSERT INTO game_type_warrior_records
                  (user_id, reached_wave, completed_wave_count, score, max_combo, solved_word_count,
                   total_kill_count, typed_letter_count, duration_seconds, effective_typing_seconds)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setInt(2, safeInt(request.reachedWave()));
            ps.setInt(3, safeInt(request.completedWaveCount()));
            ps.setLong(4, safeLong(request.score()));
            ps.setInt(5, safeInt(request.maxCombo()));
            ps.setInt(6, safeInt(request.solvedWordCount()));
            ps.setInt(7, safeInt(request.totalKillCount()));
            ps.setInt(8, safeInt(request.typedLetterCount()));
            ps.setDouble(9, safeDouble(request.durationSeconds()));
            ps.setDouble(10, safeDouble(request.effectiveTypingSeconds()));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    /**
     * 查询阶梯跳跃游戏聚合统计
     *
     * @param userId 用户ID
     * @return 聚合统计数据
     */
    public LadderJumpAggregateRow findLadderJumpAggregate(long userId) {
        String sql = """
                SELECT COUNT(*) AS session_count,
                       COALESCE(SUM(total_coins), 0) AS total_coins,
                       COALESCE(MAX(total_coins), 0) AS best_coins,
                       COALESCE(AVG(total_coins), 0) AS avg_coins,
                       COALESCE(SUM(correct_count), 0) AS total_correct,
                       COALESCE(MAX(correct_count), 0) AS best_correct,
                       COALESCE(AVG(correct_count), 0) AS avg_correct,
                       COALESCE(SUM(duration_seconds), 0) AS total_duration_seconds,
                       COALESCE(MAX(duration_seconds), 0) AS best_duration_seconds,
                       COALESCE(AVG(duration_seconds), 0) AS avg_duration_seconds
                FROM game_ladder_jump_records
                WHERE user_id = ?
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new LadderJumpAggregateRow(
                rs.getLong("session_count"),
                rs.getLong("total_coins"),
                rs.getLong("best_coins"),
                rs.getDouble("avg_coins"),
                rs.getLong("total_correct"),
                rs.getLong("best_correct"),
                rs.getDouble("avg_correct"),
                rs.getDouble("total_duration_seconds"),
                rs.getDouble("best_duration_seconds"),
                rs.getDouble("avg_duration_seconds")
        ), userId);
    }

    /**
     * 查询打字勇士游戏聚合统计
     *
     * @param userId 用户ID
     * @return 聚合统计数据
     */
    public TypeWarriorAggregateRow findTypeWarriorAggregate(long userId) {
        String sql = """
                SELECT COUNT(*) AS session_count,
                       COALESCE(SUM(score), 0) AS total_score,
                       COALESCE(MAX(score), 0) AS best_score,
                       COALESCE(AVG(score), 0) AS avg_score,
                       COALESCE(SUM(ROUND(score / 100)), 0) AS total_coins,
                       COALESCE(MAX(ROUND(score / 100)), 0) AS best_coins,
                       COALESCE(AVG(ROUND(score / 100)), 0) AS avg_coins,
                       COALESCE(SUM(total_kill_count), 0) AS total_kills,
                       COALESCE(MAX(total_kill_count), 0) AS best_kills,
                       COALESCE(AVG(total_kill_count), 0) AS avg_kills,
                       COALESCE(SUM(reached_wave), 0) AS total_reached_wave,
                       COALESCE(MAX(reached_wave), 0) AS best_reached_wave,
                       COALESCE(AVG(reached_wave), 0) AS avg_reached_wave,
                       COALESCE(SUM(duration_seconds), 0) AS total_duration_seconds,
                       COALESCE(MAX(duration_seconds), 0) AS best_duration_seconds,
                       COALESCE(AVG(duration_seconds), 0) AS avg_duration_seconds
                FROM game_type_warrior_records
                WHERE user_id = ?
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new TypeWarriorAggregateRow(
                rs.getLong("session_count"),
                rs.getLong("total_score"),
                rs.getLong("best_score"),
                rs.getDouble("avg_score"),
                rs.getLong("total_coins"),
                rs.getLong("best_coins"),
                rs.getDouble("avg_coins"),
                rs.getLong("total_kills"),
                rs.getLong("best_kills"),
                rs.getDouble("avg_kills"),
                rs.getLong("total_reached_wave"),
                rs.getLong("best_reached_wave"),
                rs.getDouble("avg_reached_wave"),
                rs.getDouble("total_duration_seconds"),
                rs.getDouble("best_duration_seconds"),
                rs.getDouble("avg_duration_seconds")
        ), userId);
    }

    /**
     * 查询所有游戏的合并时长聚合统计
     *
     * @param userId 用户ID
     * @return 合并时长聚合统计数据
     */
    public CombinedDurationAggregateRow findCombinedDurationAggregate(long userId) {
        String sql = """
                SELECT COUNT(*) AS session_count,
                       COALESCE(SUM(duration_seconds), 0) AS total_duration_seconds,
                       COALESCE(MAX(duration_seconds), 0) AS best_duration_seconds,
                       COALESCE(AVG(duration_seconds), 0) AS avg_duration_seconds
                FROM (
                    SELECT duration_seconds
                    FROM game_ladder_jump_records
                    WHERE user_id = ?
                    UNION ALL
                    SELECT duration_seconds
                    FROM game_type_warrior_records
                    WHERE user_id = ?
                ) records
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new CombinedDurationAggregateRow(
                rs.getLong("session_count"),
                rs.getDouble("total_duration_seconds"),
                rs.getDouble("best_duration_seconds"),
                rs.getDouble("avg_duration_seconds")
        ), userId, userId);
    }

    /**
     * 将空白字符串转换为null
     *
     * @param value 字符串值
     * @return 去除首尾空格后的字符串，为空则返回null
     */
    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 安全获取整数值（空值或负值时返回0）
     *
     * @param value 整数值
     * @return 非负整数值
     */
    private int safeInt(Integer value) {
        return Math.max(0, value == null ? 0 : value);
    }

    /**
     * 安全获取长整型值（空值或负值时返回0）
     *
     * @param value 长整型值
     * @return 非负长整型值
     */
    private long safeLong(Long value) {
        return Math.max(0L, value == null ? 0L : value);
    }

    /**
     * 安全获取双精度浮点值（空值或负值时返回0）
     *
     * @param value 双精度浮点值
     * @return 非负双精度浮点值
     */
    private double safeDouble(Double value) {
        return Math.max(0D, value == null ? 0D : value);
    }

    /**
     * 阶梯跳跃游戏聚合统计行
     */
    public record LadderJumpAggregateRow(
            long sessionCount,
            long totalCoins,
            long bestCoins,
            double averageCoins,
            long totalCorrect,
            long bestCorrect,
            double averageCorrect,
            double totalDurationSeconds,
            double bestDurationSeconds,
            double averageDurationSeconds
    ) {
    }

    /**
     * 打字勇士游戏聚合统计行
     */
    public record TypeWarriorAggregateRow(
            long sessionCount,
            long totalScore,
            long bestScore,
            double averageScore,
            long totalCoins,
            long bestCoins,
            double averageCoins,
            long totalKills,
            long bestKills,
            double averageKills,
            long totalReachedWave,
            long bestReachedWave,
            double averageReachedWave,
            double totalDurationSeconds,
            double bestDurationSeconds,
            double averageDurationSeconds
    ) {
    }

    /**
     * 所有游戏合并时长聚合统计行
     */
    public record CombinedDurationAggregateRow(
            long sessionCount,
            double totalDurationSeconds,
            double bestDurationSeconds,
            double averageDurationSeconds
    ) {
    }
}
