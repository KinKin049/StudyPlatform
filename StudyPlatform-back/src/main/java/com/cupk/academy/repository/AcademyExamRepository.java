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
public class AcademyExamRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AcademyExamRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<ExamSummaryRow> findExams(Long userId) {
        String sql = """
                SELECT e.exam_code, e.course_title, e.exam_title, e.teacher_name,
                       e.exam_status, e.starts_at, e.deadline_at, e.attempts_limit,
                       e.duration_minutes, e.total_score, e.exam_description,
                       COUNT(q.id) AS question_count,
                       latest.submission_status, latest.score, latest.started_at, latest.submitted_at,
                       EXISTS (
                         SELECT 1 FROM academy_exam_questions review_q
                         WHERE review_q.exam_id = e.id AND review_q.requires_teacher_review = 1
                       ) AS pending_teacher_review
                FROM academy_exams e
                LEFT JOIN academy_exam_questions q ON q.exam_id = e.id
                LEFT JOIN academy_exam_submissions latest
                  ON latest.id = (
                    SELECT s.id
                    FROM academy_exam_submissions s
                    WHERE s.exam_id = e.id AND s.user_id = ?
                    ORDER BY s.id DESC
                    LIMIT 1
                  )
                GROUP BY e.id, latest.id
                ORDER BY e.starts_at ASC, e.deadline_at DESC, e.id DESC
                """;
        return jdbcTemplate.query(sql, examSummaryMapper(), userId);
    }

    public Optional<ExamDetailRow> findExamByCode(String examCode) {
        String sql = """
                SELECT id, exam_code, course_title, exam_title, teacher_name,
                       exam_status, starts_at, deadline_at, attempts_limit,
                       duration_minutes, total_score, exam_description
                FROM academy_exams
                WHERE exam_code = ?
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, examDetailMapper(), examCode));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<ExamQuestionRow> findQuestions(Long examId) {
        String sql = """
                SELECT id, question_type, question_label, question_title, question_options,
                       placeholder_text, score, correct_answer, answer_explanation,
                       auto_gradable, oj_problem_id, requires_teacher_review
                FROM academy_exam_questions
                WHERE exam_id = ?
                ORDER BY question_order ASC
                """;
        return jdbcTemplate.query(sql, examQuestionMapper(), examId);
    }

    public Map<String, Object> findLatestAnswers(Long examId, Long userId) {
        String sql = """
                SELECT answer_payload
                FROM academy_exam_submissions
                WHERE exam_id = ? AND user_id = ?
                ORDER BY id DESC
                LIMIT 1
                """;
        try {
            String payload = jdbcTemplate.queryForObject(sql, String.class, examId, userId);
            return readObjectMap(payload);
        } catch (EmptyResultDataAccessException ex) {
            return Map.of();
        }
    }

    public Optional<SubmissionStatusRow> findLatestSubmissionStatus(Long examId, Long userId) {
        String sql = """
                SELECT submission_status, score, started_at, submitted_at
                FROM academy_exam_submissions
                WHERE exam_id = ? AND user_id = ?
                ORDER BY id DESC
                LIMIT 1
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sql,
                    (rs, rowNum) -> new SubmissionStatusRow(
                            rs.getString("submission_status"),
                            rs.getObject("score", Integer.class),
                            readDateTime(rs, "started_at"),
                            readDateTime(rs, "submitted_at")
                    ),
                    examId,
                    userId
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public void startExam(Long examId, Long userId) {
        String sql = """
                INSERT INTO academy_exam_submissions
                  (exam_id, user_id, submission_status, answer_payload, started_at)
                VALUES (?, ?, 'in_progress', JSON_OBJECT(), CURRENT_TIMESTAMP)
                """;
        jdbcTemplate.update(sql, examId, userId);
    }

    public void saveDraft(Long examId, Long userId, Map<String, Object> answers, LocalDateTime startedAt) {
        String sql = """
                INSERT INTO academy_exam_submissions
                  (exam_id, user_id, submission_status, answer_payload, started_at)
                VALUES (?, ?, 'draft', ?, ?)
                """;
        jdbcTemplate.update(sql, examId, userId, writeJson(answers), startedAt);
    }

    public void saveSubmission(
            Long examId,
            Long userId,
            Map<String, Object> answers,
            Integer score,
            boolean pendingTeacherReview,
            LocalDateTime startedAt
    ) {
        String sql = """
                INSERT INTO academy_exam_submissions
                  (exam_id, user_id, submission_status, answer_payload, score, teacher_feedback, started_at, submitted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        jdbcTemplate.update(
                sql,
                examId,
                userId,
                pendingTeacherReview ? "pending_review" : "graded",
                writeJson(answers),
                score,
                pendingTeacherReview ? "主观题或编程题待教师审核" : null,
                startedAt
        );
    }

    private RowMapper<ExamSummaryRow> examSummaryMapper() {
        return (rs, rowNum) -> new ExamSummaryRow(
                rs.getString("exam_code"),
                rs.getString("course_title"),
                rs.getString("exam_title"),
                rs.getString("teacher_name"),
                rs.getString("exam_status"),
                readDateTime(rs, "starts_at"),
                readDateTime(rs, "deadline_at"),
                rs.getObject("attempts_limit", Integer.class),
                rs.getObject("duration_minutes", Integer.class),
                rs.getObject("total_score", Integer.class),
                rs.getString("exam_description"),
                rs.getObject("question_count", Integer.class),
                rs.getString("submission_status"),
                rs.getObject("score", Integer.class),
                rs.getBoolean("pending_teacher_review"),
                readDateTime(rs, "started_at"),
                readDateTime(rs, "submitted_at")
        );
    }

    private RowMapper<ExamDetailRow> examDetailMapper() {
        return (rs, rowNum) -> new ExamDetailRow(
                rs.getLong("id"),
                rs.getString("exam_code"),
                rs.getString("course_title"),
                rs.getString("exam_title"),
                rs.getString("teacher_name"),
                rs.getString("exam_status"),
                readDateTime(rs, "starts_at"),
                readDateTime(rs, "deadline_at"),
                rs.getObject("attempts_limit", Integer.class),
                rs.getObject("duration_minutes", Integer.class),
                rs.getObject("total_score", Integer.class),
                rs.getString("exam_description")
        );
    }

    private RowMapper<ExamQuestionRow> examQuestionMapper() {
        return (rs, rowNum) -> new ExamQuestionRow(
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

    public record ExamSummaryRow(
            String code,
            String courseTitle,
            String title,
            String teacher,
            String status,
            LocalDateTime startsAt,
            LocalDateTime deadline,
            Integer attemptsLimit,
            Integer durationMinutes,
            Integer totalScore,
            String description,
            Integer questionCount,
            String submissionStatus,
            Integer score,
            Boolean pendingTeacherReview,
            LocalDateTime startedAt,
            LocalDateTime submittedAt
    ) {
    }

    public record ExamDetailRow(
            Long id,
            String code,
            String courseTitle,
            String title,
            String teacher,
            String status,
            LocalDateTime startsAt,
            LocalDateTime deadline,
            Integer attemptsLimit,
            Integer durationMinutes,
            Integer totalScore,
            String description
    ) {
    }

    public record ExamQuestionRow(
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
            Integer score,
            LocalDateTime startedAt,
            LocalDateTime submittedAt
    ) {
    }
}
