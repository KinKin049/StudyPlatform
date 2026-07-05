package com.cupk.games.repository;

import com.cupk.games.dto.LadderJumpRecordSaveRequest;
import com.cupk.games.dto.TypeWarriorRecordSaveRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Stores per-run game records and exposes aggregate views for profile pages.
 */
@Repository
public class GameRecordRepository {
    private final JdbcTemplate jdbcTemplate;

    public GameRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertLadderJumpRecord(long userId, LadderJumpRecordSaveRequest request) {
        String sql = """
                INSERT INTO game_ladder_jump_records
                  (user_id, question_bank_code, total_coins, correct_count, wrong_count, duration_seconds)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(
                sql,
                userId,
                blankToNull(request.questionBankCode()),
                safeInt(request.totalCoins()),
                safeInt(request.correctCount()),
                safeInt(request.wrongCount()),
                safeDouble(request.durationSeconds())
        );
    }

    public void insertTypeWarriorRecord(long userId, TypeWarriorRecordSaveRequest request) {
        String sql = """
                INSERT INTO game_type_warrior_records
                  (user_id, reached_wave, completed_wave_count, score, max_combo, solved_word_count,
                   total_kill_count, typed_letter_count, duration_seconds, effective_typing_seconds)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(
                sql,
                userId,
                safeInt(request.reachedWave()),
                safeInt(request.completedWaveCount()),
                safeLong(request.score()),
                safeInt(request.maxCombo()),
                safeInt(request.solvedWordCount()),
                safeInt(request.totalKillCount()),
                safeInt(request.typedLetterCount()),
                safeDouble(request.durationSeconds()),
                safeDouble(request.effectiveTypingSeconds())
        );
    }

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

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int safeInt(Integer value) {
        return Math.max(0, value == null ? 0 : value);
    }

    private long safeLong(Long value) {
        return Math.max(0L, value == null ? 0L : value);
    }

    private double safeDouble(Double value) {
        return Math.max(0D, value == null ? 0D : value);
    }

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

    public record CombinedDurationAggregateRow(
            long sessionCount,
            double totalDurationSeconds,
            double bestDurationSeconds,
            double averageDurationSeconds
    ) {
    }
}
