package com.cupk.academy.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AcademyAssignmentRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AcademyAssignmentRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<AssignmentSummaryRow> findAssignments(Long userId) {
        String sql = """
                SELECT a.assignment_code, a.course_title, a.assignment_title, a.teacher_name,
                       a.assignment_status, a.deadline_at, a.attempts_limit,
                       a.duration_minutes, a.total_score, a.assignment_description,
                       COUNT(q.id) AS question_count,
                       latest.submission_status, latest.score,
                       EXISTS (
                         SELECT 1 FROM academy_assignment_questions review_q
                         WHERE review_q.assignment_id = a.id AND review_q.requires_teacher_review = 1
                       ) AS pending_teacher_review
                FROM academy_assignments a
                LEFT JOIN academy_assignment_questions q ON q.assignment_id = a.id
                LEFT JOIN academy_assignment_submissions latest
                  ON latest.id = (
                    SELECT s.id
                    FROM academy_assignment_submissions s
                    WHERE s.assignment_id = a.id AND s.user_id = ?
                    ORDER BY s.id DESC
                    LIMIT 1
                  )
                GROUP BY a.id, latest.id
                ORDER BY a.deadline_at DESC, a.id DESC
                """;
        return jdbcTemplate.query(sql, assignmentSummaryMapper(), userId);
    }

    public Optional<AssignmentDetailRow> findAssignmentByCode(String assignmentCode) {
        String sql = """
                SELECT id, assignment_code, course_title, assignment_title, teacher_name,
                       assignment_status, deadline_at, attempts_limit, duration_minutes,
                       total_score, assignment_description
                FROM academy_assignments
                WHERE assignment_code = ?
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, assignmentDetailMapper(), assignmentCode));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<AssignmentQuestionRow> findQuestions(Long assignmentId) {
        String sql = """
                SELECT id, question_type, question_label, question_title, question_options,
                       placeholder_text, score, correct_answer, answer_explanation,
                       auto_gradable, oj_problem_id, requires_teacher_review
                FROM academy_assignment_questions
                WHERE assignment_id = ?
                ORDER BY question_order ASC
                """;
        return jdbcTemplate.query(sql, assignmentQuestionMapper(), assignmentId);
    }

    public Map<String, Object> findLatestDraftAnswers(Long assignmentId, Long userId) {
        String sql = """
                SELECT answer_payload
                FROM academy_assignment_submissions
                WHERE assignment_id = ? AND user_id = ? AND submission_status = 'draft'
                ORDER BY id DESC
                LIMIT 1
                """;
        try {
            String payload = jdbcTemplate.queryForObject(sql, String.class, assignmentId, userId);
            return readObjectMap(payload);
        } catch (EmptyResultDataAccessException ex) {
            return Map.of();
        }
    }

    public Optional<SubmissionStatusRow> findLatestSubmissionStatus(Long assignmentId, Long userId) {
        String sql = """
                SELECT submission_status, score
                FROM academy_assignment_submissions
                WHERE assignment_id = ? AND user_id = ?
                ORDER BY id DESC
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sql,
                    (rs, rowNum) -> new SubmissionStatusRow(rs.getString("submission_status"), rs.getObject("score", Integer.class)),
                    assignmentId,
                    userId
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public void saveDraft(Long assignmentId, Long userId, Map<String, Object> answers) {
        String sql = """
                INSERT INTO academy_assignment_submissions
                  (assignment_id, user_id, submission_status, answer_payload)
                VALUES (?, ?, 'draft', ?)
                """;
        jdbcTemplate.update(sql, assignmentId, userId, writeJson(answers));
    }

    public void saveSubmission(Long assignmentId, Long userId, Map<String, Object> answers, Integer score, boolean pendingTeacherReview) {
        String sql = """
                INSERT INTO academy_assignment_submissions
                  (assignment_id, user_id, submission_status, answer_payload, score, teacher_feedback, submitted_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        jdbcTemplate.update(
                sql,
                assignmentId,
                userId,
                pendingTeacherReview ? "pending_review" : "graded",
                writeJson(answers),
                score,
                pendingTeacherReview ? "主观题或编程题待教师审核" : null
        );
    }

    private RowMapper<AssignmentSummaryRow> assignmentSummaryMapper() {
        return (rs, rowNum) -> new AssignmentSummaryRow(
                rs.getString("assignment_code"),
                rs.getString("course_title"),
                rs.getString("assignment_title"),
                rs.getString("teacher_name"),
                rs.getString("assignment_status"),
                readDateTime(rs, "deadline_at"),
                rs.getObject("attempts_limit", Integer.class),
                rs.getObject("duration_minutes", Integer.class),
                rs.getObject("total_score", Integer.class),
                rs.getString("assignment_description"),
                rs.getObject("question_count", Integer.class),
                rs.getString("submission_status"),
                rs.getObject("score", Integer.class),
                rs.getBoolean("pending_teacher_review")
        );
    }

    private RowMapper<AssignmentDetailRow> assignmentDetailMapper() {
        return (rs, rowNum) -> new AssignmentDetailRow(
                rs.getLong("id"),
                rs.getString("assignment_code"),
                rs.getString("course_title"),
                rs.getString("assignment_title"),
                rs.getString("teacher_name"),
                rs.getString("assignment_status"),
                readDateTime(rs, "deadline_at"),
                rs.getObject("attempts_limit", Integer.class),
                rs.getObject("duration_minutes", Integer.class),
                rs.getObject("total_score", Integer.class),
                rs.getString("assignment_description")
        );
    }

    private RowMapper<AssignmentQuestionRow> assignmentQuestionMapper() {
        return (rs, rowNum) -> new AssignmentQuestionRow(
                rs.getLong("id"),
                rs.getString("question_type"),
                rs.getString("question_label"),
                rs.getString("question_title"),
                readStringList(rs.getString("question_options")),
                rs.getString("placeholder_text"),
                rs.getObject("score", Integer.class),
                readJsonValue(rs.getString("correct_answer")),
                rs.getString("answer_explanation"),
                rs.getBoolean("auto_gradable"),
                rs.getObject("oj_problem_id", Long.class),
                rs.getBoolean("requires_teacher_review")
        );
    }

    private LocalDateTime readDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Object readJsonValue(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private Map<String, Object> readObjectMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    public record AssignmentSummaryRow(
            String code,
            String courseTitle,
            String title,
            String teacher,
            String status,
            LocalDateTime deadline,
            Integer attemptsLimit,
            Integer durationMinutes,
            Integer totalScore,
            String description,
            Integer questionCount,
            String submissionStatus,
            Integer score,
            Boolean pendingTeacherReview
    ) {
    }

    public record AssignmentDetailRow(
            Long id,
            String code,
            String courseTitle,
            String title,
            String teacher,
            String status,
            LocalDateTime deadline,
            Integer attemptsLimit,
            Integer durationMinutes,
            Integer totalScore,
            String description
    ) {
    }

    public record AssignmentQuestionRow(
            Long id,
            String type,
            String label,
            String title,
            List<String> options,
            String placeholder,
            Integer score,
            Object correctAnswer,
            String explanation,
            Boolean autoGradable,
            Long ojProblemId,
            Boolean requiresTeacherReview
    ) {
    }

    public record SubmissionStatusRow(
            String status,
            Integer score
    ) {
    }
}
