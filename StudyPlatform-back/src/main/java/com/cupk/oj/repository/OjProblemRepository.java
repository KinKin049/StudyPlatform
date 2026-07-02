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
import java.util.ArrayList;
import java.util.Arrays;
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

    public List<ProblemSummary> findAll(
            ProblemStatus status,
            String keyword,
            String tags,
            String difficulties,
            String languages
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        List<String> normalizedTags = splitCsv(tags).stream()
                .map(String::toLowerCase)
                .toList();
        List<String> normalizedDifficulties = splitCsv(difficulties).stream()
                .map(String::toUpperCase)
                .toList();
        List<String> normalizedLanguages = splitCsv(languages).stream()
                .map(String::toLowerCase)
                .toList();
        List<String> keywordTags = tagsForKeyword(normalizedKeyword);
        List<String> keywordDifficulties = difficultiesForKeyword(normalizedKeyword);
        List<Object> args = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
                SELECT id, title, slug, difficulty, time_limit_ms, memory_limit_kb,
                       CAST(tags AS CHAR) AS tags, status, created_at, updated_at
                FROM oj_problems
                WHERE 1 = 1
                """);

        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status.name());
        }

        if (normalizedKeyword != null) {
            String keywordValue = "%" + normalizedKeyword + "%";
            sql.append("""
                     AND (
                       LOWER(title) LIKE ?
                       OR LOWER(slug) LIKE ?
                       OR LOWER(CAST(tags AS CHAR)) LIKE ?
                    """);
            args.add(keywordValue);
            args.add(keywordValue);
            args.add(keywordValue);
            if (!keywordDifficulties.isEmpty()) {
                sql.append(" OR difficulty IN (");
                appendPlaceholders(sql, keywordDifficulties.size());
                sql.append(")");
                args.addAll(keywordDifficulties);
            }
            for (String keywordTag : keywordTags) {
                sql.append(" OR LOWER(CAST(tags AS CHAR)) LIKE ?");
                args.add("%\"" + keywordTag + "\"%");
            }
            sql.append("""
                     )
                    """);
        }

        if (!normalizedDifficulties.isEmpty()) {
            sql.append(" AND difficulty IN (");
            appendPlaceholders(sql, normalizedDifficulties.size());
            sql.append(")");
            args.addAll(normalizedDifficulties);
        }

        if (!normalizedTags.isEmpty()) {
            sql.append(" AND (");
            for (int index = 0; index < normalizedTags.size(); index += 1) {
                if (index > 0) {
                    sql.append(" OR ");
                }
                sql.append("LOWER(CAST(tags AS CHAR)) LIKE ?");
                args.add("%\"" + normalizedTags.get(index) + "\"%");
            }
            sql.append(")");
        }

        if (normalizedLanguages.size() == 1) {
            if (normalizedLanguages.contains("zh")) {
                sql.append(" AND (title REGEXP '[一-龥]' OR description REGEXP '[一-龥]')");
            } else if (normalizedLanguages.contains("en")) {
                sql.append(" AND title NOT REGEXP '[一-龥]'");
            }
        }

        sql.append(" ORDER BY id DESC");
        return jdbcTemplate.query(sql.toString(), summaryMapper(), args.toArray());
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

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase();
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private void appendPlaceholders(StringBuilder sql, int count) {
        for (int index = 0; index < count; index += 1) {
            if (index > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
    }

    private List<String> tagsForKeyword(String keyword) {
        if (keyword == null) {
            return List.of();
        }
        return switch (keyword) {
            case "入门", "beginner" -> List.of("beginner");
            case "数学", "math" -> List.of("math");
            case "数论", "number theory", "number-theory" -> List.of("number-theory");
            case "数组", "array" -> List.of("array");
            case "字符串", "string" -> List.of("string");
            case "栈", "stack" -> List.of("stack");
            case "哈希表", "hash table", "hash-table" -> List.of("hash-table");
            case "排序", "sort" -> List.of("sort");
            case "区间", "interval" -> List.of("interval");
            case "动态规划", "dp", "dynamic programming" -> List.of("dp");
            case "二分", "binary search", "binary-search" -> List.of("binary-search");
            case "图论", "graph" -> List.of("graph");
            case "广度优先搜索", "bfs" -> List.of("bfs");
            case "网格", "grid" -> List.of("grid");
            case "筛法", "sieve" -> List.of("sieve");
            case "前缀", "prefix" -> List.of("prefix");
            default -> List.of();
        };
    }

    private List<String> difficultiesForKeyword(String keyword) {
        if (keyword == null) {
            return List.of();
        }
        return switch (keyword) {
            case "简单", "easy" -> List.of("EASY");
            case "中等", "medium" -> List.of("MEDIUM");
            case "困难", "hard" -> List.of("HARD");
            default -> List.of();
        };
    }
}
