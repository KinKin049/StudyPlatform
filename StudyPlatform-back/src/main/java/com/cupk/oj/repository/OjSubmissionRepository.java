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

@Repository
public class OjSubmissionRepository {
    private final JdbcTemplate jdbcTemplate;

    public OjSubmissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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

    public Optional<OjSubmission> findById(Long id) {
        return jdbcTemplate.query("""
                SELECT id, problem_id, user_id, language, source_code, status, score,
                       time_used_ms, memory_used_kb, message, judged_at, created_at, updated_at
                FROM oj_submissions
                WHERE id = ?
                """, mapper(), id).stream().findFirst();
    }

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

    public int updateStatus(Long id, SubmissionStatus status, String message) {
        return jdbcTemplate.update("""
                UPDATE oj_submissions
                SET status = ?, message = ?
                WHERE id = ?
                """, status.name(), message, id);
    }

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

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

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
