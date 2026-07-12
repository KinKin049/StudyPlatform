package com.cupk.oj.repository;

import com.cupk.oj.dto.CreateSubmissionRequest;
import com.cupk.oj.dto.JudgeResult;
import com.cupk.oj.model.OjSubmission;
import com.cupk.oj.model.SubmissionStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 在线判题系统提交记录数据访问层，提供提交创建、查询、状态更新和判题结果记录等功能。
 */
@Repository
public class OjSubmissionRepository {
    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造函数
     *
     * @param jdbcTemplate JDBC模板
     */
    public OjSubmissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 创建新的代码提交记录
     *
     * @param request 创建提交请求
     * @return 提交ID
     */
    public Long create(CreateSubmissionRequest request) {
        String sql = """
                INSERT INTO oj_submissions (problem_id, user_id, language, source_code)
                VALUES (?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.problemId());
            if (request.userId() == null) {
                ps.setObject(2, null);
            } else {
                ps.setLong(2, request.userId());
            }
            ps.setString(3, request.language());
            ps.setString(4, request.sourceCode());
            return ps;
        }, keyHolder);
        return generatedId(keyHolder);
    }

    /**
     * 根据ID查询提交记录
     *
     * @param id 提交ID
     * @return 提交记录，不存在则返回空
     */
    public Optional<OjSubmission> findById(Long id) {
        return jdbcTemplate.query("""
                SELECT id, problem_id, user_id, language, source_code, status, score,
                       time_used_ms, memory_used_kb, message, judged_at, created_at, updated_at
                FROM oj_submissions
                WHERE id = ?
                """, mapper(), id).stream().findFirst();
    }

    /**
     * 根据题目ID查询提交记录列表（最多100条）
     *
     * @param problemId 题目ID
     * @return 提交记录列表
     */
    public List<OjSubmission> findByProblemId(Long problemId) {
        return jdbcTemplate.query("""
                SELECT id, problem_id, user_id, language, source_code, status, score,
                       time_used_ms, memory_used_kb, message, judged_at, created_at, updated_at
                FROM oj_submissions
                WHERE problem_id = ?
                ORDER BY id DESC
                LIMIT 100
                """, mapper(), problemId);
    }

    /**
     * 更新提交状态
     *
     * @param id 提交ID
     * @param status 状态
     * @param message 状态消息
     * @return 更新的行数
     */
    public int updateStatus(Long id, SubmissionStatus status, String message) {
        return jdbcTemplate.update("""
                UPDATE oj_submissions
                SET status = ?, message = ?
                WHERE id = ?
                """, status.name(), message, id);
    }

    /**
     * 更新判题结果
     *
     * @param id 提交ID
     * @param result 判题结果
     * @return 更新的行数
     */
    public int updateJudgeResult(Long id, JudgeResult result) {
        return jdbcTemplate.update("""
                UPDATE oj_submissions
                SET status = ?, score = ?, time_used_ms = ?, memory_used_kb = ?, message = ?, judged_at = ?
                WHERE id = ?
                """,
                result.status().name(),
                result.score(),
                result.timeUsedMs(),
                result.memoryUsedKb(),
                result.message(),
                Timestamp.valueOf(LocalDateTime.now()),
                id);
    }

    /**
     * 创建提交记录行映射器
     *
     * @return RowMapper
     */
    private RowMapper<OjSubmission> mapper() {
        return (rs, rowNum) -> new OjSubmission(
                rs.getLong("id"),
                rs.getLong("problem_id"),
                getNullableLong(rs, "user_id"),
                rs.getString("language"),
                rs.getString("source_code"),
                SubmissionStatus.valueOf(rs.getString("status")),
                rs.getInt("score"),
                getNullableInt(rs, "time_used_ms"),
                getNullableInt(rs, "memory_used_kb"),
                rs.getString("message"),
                rs.getTimestamp("judged_at") == null ? null : rs.getTimestamp("judged_at").toLocalDateTime(),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    /**
     * 安全获取长整型值（空值时返回null）
     *
     * @param rs 结果集
     * @param column 列名
     * @return 长整型值，空则返回null
     * @throws SQLException SQL异常
     */
    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * 安全获取整数值（空值时返回null）
     *
     * @param rs 结果集
     * @param column 列名
     * @return 整数值，空则返回null
     * @throws SQLException SQL异常
     */
    private Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Long generatedId(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Generated key is missing");
        }
        return key.longValue();
    }
}
