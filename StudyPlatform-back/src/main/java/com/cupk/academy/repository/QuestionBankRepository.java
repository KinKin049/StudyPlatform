package com.cupk.academy.repository;

import com.cupk.academy.dto.QuestionBankProblemPageResponse;
import com.cupk.academy.dto.QuestionBankProblemResponse;
import com.cupk.academy.dto.QuestionBankSubjectResponse;
import com.cupk.academy.dto.CourseQuestionBankCategoryResponse;
import com.cupk.academy.dto.CourseQuestionBankQuestionResponse;
import com.cupk.academy.dto.CourseQuestionBankSetResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionBankRepository {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public QuestionBankRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<QuestionBankSubjectResponse> findSubjects() {
        String sql = """
                SELECT s.subject_code, s.subject_name, s.description, COUNT(ps.problem_id) AS problem_count
                FROM question_bank_subjects s
                LEFT JOIN question_bank_problem_subjects ps ON ps.subject_id = s.id
                GROUP BY s.id, s.subject_code, s.subject_name, s.description, s.sort_order
                ORDER BY s.sort_order ASC, s.id ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new QuestionBankSubjectResponse(
                rs.getString("subject_code"),
                rs.getString("subject_name"),
                rs.getString("description"),
                rs.getInt("problem_count")
        ));
    }

    public QuestionBankProblemPageResponse findProblems(
            String subjectCode,
            String keyword,
            Integer difficulty,
            int page,
            int size
    ) {
        List<Object> params = new ArrayList<>();
        String where = buildProblemWhere(subjectCode, keyword, difficulty, params);

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM question_bank_problems p " + where, Long.class, params.toArray());
        int offset = Math.max(0, page) * size;
        List<Object> listParams = new ArrayList<>(params);
        listParams.add(size);
        listParams.add(offset);
        String sql = """
                SELECT p.id, p.source, p.external_problem_id, p.title, p.difficulty, p.difficulty_label,
                       CAST(p.tag_names AS CHAR) AS tag_names, p.description, p.input_description,
                       p.output_description, p.hint, p.total_submit, p.total_accepted, p.source_url, p.imported_at
                FROM question_bank_problems p
                %s
                ORDER BY COALESCE(p.difficulty, 99) ASC, p.external_problem_id ASC
                LIMIT ? OFFSET ?
                """.formatted(where);
        List<QuestionBankProblemResponse> items = jdbcTemplate.query(sql, this::mapProblem, listParams.toArray());
        return new QuestionBankProblemPageResponse(items, Math.max(0, page), size, total == null ? 0 : total);
    }

    public Optional<QuestionBankProblemResponse> findProblemById(long id) {
        String sql = """
                SELECT p.id, p.source, p.external_problem_id, p.title, p.difficulty, p.difficulty_label,
                       CAST(p.tag_names AS CHAR) AS tag_names, p.description, p.input_description,
                       p.output_description, p.hint, p.total_submit, p.total_accepted, p.source_url, p.imported_at
                FROM question_bank_problems p
                WHERE p.id = ?
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, this::mapProblem, id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public int upsertTag(String source, int externalTagId, String tagName, Integer tagType, Integer parentExternalTagId) {
        String sql = """
                INSERT INTO question_bank_tags (source, external_tag_id, tag_name, tag_type, parent_external_tag_id)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  tag_name = VALUES(tag_name),
                  tag_type = VALUES(tag_type),
                  parent_external_tag_id = VALUES(parent_external_tag_id)
                """;
        return jdbcTemplate.update(sql, source, externalTagId, tagName, tagType, parentExternalTagId);
    }

    public long upsertProblem(
            String source,
            String externalProblemId,
            String title,
            Integer difficulty,
            String difficultyLabel,
            List<Integer> tagIds,
            List<String> tagNames,
            String description,
            String inputDescription,
            String outputDescription,
            String hint,
            Integer totalSubmit,
            Integer totalAccepted,
            String sourceUrl
    ) {
        String sql = """
                INSERT INTO question_bank_problems
                  (source, external_problem_id, title, difficulty, difficulty_label, tag_ids, tag_names,
                   description, input_description, output_description, hint, total_submit, total_accepted, source_url)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  title = VALUES(title),
                  difficulty = VALUES(difficulty),
                  difficulty_label = VALUES(difficulty_label),
                  tag_ids = VALUES(tag_ids),
                  tag_names = VALUES(tag_names),
                  description = COALESCE(NULLIF(VALUES(description), ''), description),
                  input_description = COALESCE(NULLIF(VALUES(input_description), ''), input_description),
                  output_description = COALESCE(NULLIF(VALUES(output_description), ''), output_description),
                  hint = COALESCE(NULLIF(VALUES(hint), ''), hint),
                  total_submit = VALUES(total_submit),
                  total_accepted = VALUES(total_accepted),
                  source_url = VALUES(source_url)
                """;
        jdbcTemplate.update(sql, source, externalProblemId, title, difficulty, difficultyLabel,
                toJson(tagIds), toJson(tagNames), description, inputDescription, outputDescription, hint,
                totalSubmit, totalAccepted, sourceUrl);

        return jdbcTemplate.queryForObject(
                "SELECT id FROM question_bank_problems WHERE source = ? AND external_problem_id = ?",
                Long.class,
                source,
                externalProblemId
        );
    }

    public void attachProblemToAllSubjects(long problemId) {
        String sql = """
                INSERT IGNORE INTO question_bank_problem_subjects (problem_id, subject_id)
                SELECT ?, id FROM question_bank_subjects
                WHERE subject_code IN ('c-language', 'java', 'python')
                """;
        jdbcTemplate.update(sql, problemId);
    }

    public List<String> findTagNames(String source, List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", tagIds.stream().map(id -> "?").toList());
        List<Object> params = new ArrayList<>();
        params.add(source);
        params.addAll(tagIds);
        String sql = "SELECT tag_name FROM question_bank_tags WHERE source = ? AND external_tag_id IN (" + placeholders + ") ORDER BY id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("tag_name"), params.toArray());
    }

    public List<CourseQuestionBankCategoryResponse> findCourseQuestionBankCatalog() {
        String sql = """
                SELECT c.category_code, c.category_name, c.description AS category_description,
                       s.id, s.set_code, s.title, s.subtitle, s.description, s.cover_url, s.cover_file_path,
                       s.difficulty_label, s.status_label, s.source_name, s.source_url,
                       CAST(s.source_refs AS CHAR) AS source_refs, s.route_path,
                       (
                         SELECT COUNT(*)
                         FROM course_question_bank_questions q
                         WHERE q.set_id = s.id
                       ) AS question_count
                FROM course_question_bank_categories c
                LEFT JOIN course_question_bank_sets s ON s.category_id = c.id
                ORDER BY c.sort_order ASC, c.id ASC, s.sort_order ASC, s.id ASC
                """;
        Map<String, CourseQuestionBankCategoryBuilder> categories = new LinkedHashMap<>();
        jdbcTemplate.query(sql, rs -> {
            String categoryCode = rs.getString("category_code");
            CourseQuestionBankCategoryBuilder builder = categories.computeIfAbsent(
                    categoryCode,
                    code -> new CourseQuestionBankCategoryBuilder(
                            code,
                            getString(rs, "category_name"),
                            getString(rs, "category_description")
                    )
            );
            long setId = rs.getLong("id");
            if (!rs.wasNull()) {
                builder.sets().add(mapCourseQuestionBankSet(rs));
            }
        });
        return categories.values().stream()
                .map(builder -> new CourseQuestionBankCategoryResponse(
                        builder.code(),
                        builder.name(),
                        builder.description(),
                        builder.sets()
                ))
                .toList();
    }

    public Optional<CourseQuestionBankSetResponse> findCourseQuestionBankSet(String code) {
        String sql = """
                SELECT c.category_code, c.category_name, c.description AS category_description,
                       s.id, s.set_code, s.title, s.subtitle, s.description, s.cover_url, s.cover_file_path,
                       s.difficulty_label, s.status_label, s.source_name, s.source_url,
                       CAST(s.source_refs AS CHAR) AS source_refs, s.route_path,
                       (
                         SELECT COUNT(*)
                         FROM course_question_bank_questions q
                         WHERE q.set_id = s.id
                       ) AS question_count
                FROM course_question_bank_sets s
                JOIN course_question_bank_categories c ON c.id = s.category_id
                WHERE s.set_code = ?
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, this::mapCourseQuestionBankSet, code));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<CourseQuestionBankQuestionResponse> findCourseQuestionBankQuestions(String code) {
        String sql = """
                SELECT q.id, q.question_type, q.stem, CAST(q.options_json AS CHAR) AS options_json,
                       q.answer, q.explanation, q.difficulty_label, q.source_url
                FROM course_question_bank_questions q
                JOIN course_question_bank_sets s ON s.id = q.set_id
                WHERE s.set_code = ?
                ORDER BY q.sort_order ASC, q.id ASC
                """;
        return jdbcTemplate.query(sql, this::mapCourseQuestionBankQuestion, code);
    }

    private String buildProblemWhere(String subjectCode, String keyword, Integer difficulty, List<Object> params) {
        List<String> conditions = new ArrayList<>();
        if (subjectCode != null && !subjectCode.isBlank()) {
            conditions.add("""
                    EXISTS (
                      SELECT 1
                      FROM question_bank_problem_subjects ps
                      JOIN question_bank_subjects s ON s.id = ps.subject_id
                      WHERE ps.problem_id = p.id AND s.subject_code = ?
                    )
                    """);
            params.add(subjectCode);
        }
        if (keyword != null && !keyword.isBlank()) {
            conditions.add("(p.title LIKE ? OR p.external_problem_id LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (difficulty != null) {
            conditions.add("p.difficulty = ?");
            params.add(difficulty);
        }
        return conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);
    }

    private QuestionBankProblemResponse mapProblem(ResultSet rs, int rowNum) throws SQLException {
        return new QuestionBankProblemResponse(
                rs.getLong("id"),
                rs.getString("source"),
                rs.getString("external_problem_id"),
                rs.getString("title"),
                rs.getObject("difficulty", Integer.class),
                rs.getString("difficulty_label"),
                parseStringList(rs.getString("tag_names")),
                rs.getString("description"),
                rs.getString("input_description"),
                rs.getString("output_description"),
                rs.getString("hint"),
                rs.getObject("total_submit", Integer.class),
                rs.getObject("total_accepted", Integer.class),
                rs.getString("source_url"),
                rs.getObject("imported_at", LocalDateTime.class)
        );
    }

    private CourseQuestionBankSetResponse mapCourseQuestionBankSet(ResultSet rs, int rowNum) throws SQLException {
        return mapCourseQuestionBankSet(rs);
    }

    private CourseQuestionBankSetResponse mapCourseQuestionBankSet(ResultSet rs) throws SQLException {
        return new CourseQuestionBankSetResponse(
                rs.getLong("id"),
                rs.getString("set_code"),
                rs.getString("title"),
                rs.getString("subtitle"),
                rs.getString("description"),
                rs.getString("category_code"),
                rs.getString("category_name"),
                rs.getString("cover_url"),
                rs.getString("cover_url"),
                rs.getString("cover_file_path"),
                rs.getInt("question_count"),
                rs.getString("difficulty_label"),
                rs.getString("status_label"),
                rs.getString("source_name"),
                rs.getString("source_url"),
                parseStringList(rs.getString("source_refs")),
                rs.getString("route_path")
        );
    }

    private CourseQuestionBankQuestionResponse mapCourseQuestionBankQuestion(ResultSet rs, int rowNum) throws SQLException {
        return new CourseQuestionBankQuestionResponse(
                rs.getLong("id"),
                rs.getString("question_type"),
                rs.getString("stem"),
                parseStringList(rs.getString("options_json")),
                rs.getString("answer"),
                rs.getString("explanation"),
                rs.getString("difficulty_label"),
                rs.getString("source_url")
        );
    }

    private String getString(ResultSet rs, String columnLabel) {
        try {
            return rs.getString(columnLabel);
        } catch (SQLException ex) {
            return "";
        }
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private record CourseQuestionBankCategoryBuilder(
            String code,
            String name,
            String description,
            List<CourseQuestionBankSetResponse> sets
    ) {
        private CourseQuestionBankCategoryBuilder(String code, String name, String description) {
            this(code, name, description, new ArrayList<>());
        }
    }
}
