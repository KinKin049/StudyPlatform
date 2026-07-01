package com.cupk.oj.repository;

import com.cupk.oj.dto.JudgeCaseResult;
import com.cupk.oj.model.OjSubmissionCase;
import com.cupk.oj.model.SubmissionStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class OjSubmissionCaseRepository {
    private final JdbcTemplate jdbcTemplate;

    public OjSubmissionCaseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createAll(Long submissionId, List<JudgeCaseResult> results) {
        jdbcTemplate.batchUpdate("""
                INSERT INTO oj_submission_cases
                (submission_id, test_case_id, status, time_used_ms, memory_used_kb, message)
                VALUES (?, ?, ?, ?, ?, ?)
                """, results, 100, (ps, result) -> {
            ps.setLong(1, submissionId);
            ps.setLong(2, result.testCaseId());
            ps.setString(3, result.status().name());
            ps.setObject(4, result.timeUsedMs());
            ps.setObject(5, result.memoryUsedKb());
            ps.setString(6, result.message());
        });
    }

    public List<OjSubmissionCase> findBySubmissionId(Long submissionId) {
        return jdbcTemplate.query("""
                SELECT id, submission_id, test_case_id, status, time_used_ms, memory_used_kb, message, created_at
                FROM oj_submission_cases
                WHERE submission_id = ?
                ORDER BY id ASC
                """, mapper(), submissionId);
    }

    private RowMapper<OjSubmissionCase> mapper() {
        return (rs, rowNum) -> new OjSubmissionCase(
                rs.getLong("id"),
                rs.getLong("submission_id"),
                rs.getLong("test_case_id"),
                SubmissionStatus.valueOf(rs.getString("status")),
                getNullableInt(rs, "time_used_ms"),
                getNullableInt(rs, "memory_used_kb"),
                rs.getString("message"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
