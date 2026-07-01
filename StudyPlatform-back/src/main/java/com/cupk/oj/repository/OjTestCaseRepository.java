package com.cupk.oj.repository;

import com.cupk.oj.dto.CreateTestCaseRequest;
import com.cupk.oj.model.OjTestCase;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class OjTestCaseRepository {
    private final JdbcTemplate jdbcTemplate;

    public OjTestCaseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long create(Long problemId, CreateTestCaseRequest request) {
        String sql = """
                INSERT INTO oj_test_cases (problem_id, input_data, expected_output, sample, weight, sort_order)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, problemId);
            ps.setString(2, request.inputData());
            ps.setString(3, request.expectedOutput());
            ps.setBoolean(4, request.sample());
            ps.setInt(5, request.weight());
            ps.setInt(6, request.sortOrder());
            return ps;
        }, keyHolder);
        return generatedId(keyHolder);
    }

    public List<OjTestCase> findByProblemId(Long problemId) {
        return jdbcTemplate.query("""
                SELECT id, problem_id, input_data, expected_output, sample, weight, sort_order, created_at
                FROM oj_test_cases
                WHERE problem_id = ?
                ORDER BY sort_order ASC, id ASC
                """, mapper(), problemId);
    }

    public Optional<OjTestCase> findById(Long id) {
        return jdbcTemplate.query("""
                SELECT id, problem_id, input_data, expected_output, sample, weight, sort_order, created_at
                FROM oj_test_cases
                WHERE id = ?
                """, mapper(), id).stream().findFirst();
    }

    public int delete(Long id) {
        return jdbcTemplate.update("DELETE FROM oj_test_cases WHERE id = ?", id);
    }

    private RowMapper<OjTestCase> mapper() {
        return (rs, rowNum) -> new OjTestCase(
                rs.getLong("id"),
                rs.getLong("problem_id"),
                rs.getString("input_data"),
                rs.getString("expected_output"),
                rs.getBoolean("sample"),
                rs.getInt("weight"),
                rs.getInt("sort_order"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private Long generatedId(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Generated key is missing");
        }
        return key.longValue();
    }
}
