package com.cupk.oj.repository;

import com.cupk.oj.dto.CreateProblemRequest;
import com.cupk.oj.dto.ProblemSummary;
import com.cupk.oj.dto.UpdateProblemRequest;
import com.cupk.oj.model.OjProblem;
import com.cupk.oj.model.ProblemDifficulty;
import com.cupk.oj.model.ProblemStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class OjProblemRepository {
    private final JdbcTemplate jdbcTemplate;

    public OjProblemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long create(CreateProblemRequest request) {
        String sql = """
                INSERT INTO oj_problems
                (title, slug, description, input_description, output_description, samples, difficulty,
                 time_limit_ms, memory_limit_kb, tags, status, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, request.title());
            ps.setString(2, request.slug());
            ps.setString(3, request.description());
            ps.setString(4, request.inputDescription());
            ps.setString(5, request.outputDescription());
            ps.setString(6, request.samples());
            ps.setString(7, request.difficulty().name());
            ps.setInt(8, request.timeLimitMs());
            ps.setInt(9, request.memoryLimitKb());
            ps.setString(10, request.tags());
            ps.setString(11, request.status().name());
            if (request.createdBy() == null) {
                ps.setObject(12, null);
            } else {
                ps.setLong(12, request.createdBy());
            }
            return ps;
        }, keyHolder);
        return generatedId(keyHolder);
    }

    public int update(Long id, UpdateProblemRequest request) {
        String sql = """
                UPDATE oj_problems
                SET title = ?, description = ?, input_description = ?, output_description = ?,
                    samples = ?, difficulty = ?, time_limit_ms = ?, memory_limit_kb = ?,
                    tags = ?, status = ?
                WHERE id = ?
                """;
        return jdbcTemplate.update(sql,
                request.title(),
                request.description(),
                request.inputDescription(),
                request.outputDescription(),
                request.samples(),
                request.difficulty().name(),
                request.timeLimitMs(),
                request.memoryLimitKb(),
                request.tags(),
                request.status().name(),
                id);
    }

    public List<ProblemSummary> findAll(ProblemStatus status) {
        String baseSql = """
                SELECT id, title, slug, difficulty, time_limit_ms, memory_limit_kb,
                       CAST(tags AS CHAR) AS tags, status, created_at, updated_at
                FROM oj_problems
                """;
        if (status == null) {
            return jdbcTemplate.query(baseSql + " ORDER BY id DESC", summaryMapper());
        }
        return jdbcTemplate.query(baseSql + " WHERE status = ? ORDER BY id DESC", summaryMapper(), status.name());
    }

    public Optional<OjProblem> findById(Long id) {
        String sql = """
                SELECT id, title, slug, description, input_description, output_description,
                       CAST(samples AS CHAR) AS samples, difficulty, time_limit_ms, memory_limit_kb,
                       CAST(tags AS CHAR) AS tags, status, created_by, created_at, updated_at
                FROM oj_problems
                WHERE id = ?
                """;
        return jdbcTemplate.query(sql, problemMapper(), id).stream().findFirst();
    }

    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oj_problems WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private RowMapper<OjProblem> problemMapper() {
        return (rs, rowNum) -> new OjProblem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("slug"),
                rs.getString("description"),
                rs.getString("input_description"),
                rs.getString("output_description"),
                rs.getString("samples"),
                ProblemDifficulty.valueOf(rs.getString("difficulty")),
                rs.getInt("time_limit_ms"),
                rs.getInt("memory_limit_kb"),
                rs.getString("tags"),
                ProblemStatus.valueOf(rs.getString("status")),
                getNullableLong(rs, "created_by"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    private RowMapper<ProblemSummary> summaryMapper() {
        return (rs, rowNum) -> new ProblemSummary(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("slug"),
                ProblemDifficulty.valueOf(rs.getString("difficulty")),
                rs.getInt("time_limit_ms"),
                rs.getInt("memory_limit_kb"),
                rs.getString("tags"),
                ProblemStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
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
