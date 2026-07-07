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

    /**
     * 考试数据访问层，提供考试查询、题目管理、答题提交和成绩记录等功能。
     */

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     *
     * @param jdbcTemplate JDBC模板
     * @param objectMapper JSON序列化器
     */
    public AcademyExamRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询考试列表（含用户最新提交状态）
     *
     * @param userId 用户ID
     * @return 考试列表
     */
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

    /**
     * 根据代码查询考试详情
     *
     * @param examCode 考试代码
     * @return 考试详情，不存在则返回空
     */
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

    /**
     * 查询考试题目列表
     *
     * @param examId 考试ID
     * @return 题目列表
     */
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

    /**
     * 查询用户最新答题记录
     *
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 答题记录（JSON反序列化后的Map）
     */
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

    /**
     * 查询用户最新提交状态
     *
     * @param examId 考试ID
     * @param userId 用户ID
     * @return 提交状态，不存在则返回空
     */
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

    /**
     * 开始考试（创建提交记录）
     *
     * @param examId 考试ID
     * @param userId 用户ID
     */
    public void startExam(Long examId, Long userId) {
        String sql = """
                INSERT INTO academy_exam_submissions
                  (exam_id, user_id, submission_status, answer_payload, started_at)
                VALUES (?, ?, 'in_progress', JSON_OBJECT(), CURRENT_TIMESTAMP)
                """;
        jdbcTemplate.update(sql, examId, userId);
    }

    /**
     * 保存考试草稿
     *
     * @param examId 考试ID
     * @param userId 用户ID
     * @param answers 答案Map
     * @param startedAt 开始时间
     */
    public void saveDraft(Long examId, Long userId, Map<String, Object> answers, LocalDateTime startedAt) {
        String sql = """
                INSERT INTO academy_exam_submissions
                  (exam_id, user_id, submission_status, answer_payload, started_at)
                VALUES (?, ?, 'draft', ?, ?)
                """;
        jdbcTemplate.update(sql, examId, userId, writeJson(answers), startedAt);
    }

    /**
     * 提交考试答案
     *
     * @param examId 考试ID
     * @param userId 用户ID
     * @param answers 答案Map
     * @param score 分数
     * @param pendingTeacherReview 是否待教师审核
     * @param startedAt 开始时间
     */
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

    /**
     * 创建考试汇总行映射器
     *
     * @return RowMapper
     */
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

    /**
     * 创建考试详情行映射器
     *
     * @return RowMapper
     */
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

    /**
     * 创建考试题目行映射器
     *
     * @return RowMapper
     */
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

    /**
     * 从结果集读取LocalDateTime
     *
     * @param rs 结果集
     * @param column 列名
     * @return LocalDateTime，为空则返回null
     * @throws SQLException SQL异常
     */
    private LocalDateTime readDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * 将JSON字符串解析为字符串列表
     *
     * @param json JSON字符串
     * @return 字符串列表
     */
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

    /**
     * 将JSON字符串解析为对象
     *
     * @param json JSON字符串
     * @return 对象，为空则返回null
     */
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

    /**
     * 将JSON字符串解析为Map
     *
     * @param json JSON字符串
     * @return Map，为空则返回空Map
     */
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

    /**
     * 将对象转换为JSON字符串
     *
     * @param value 对象
     * @return JSON字符串
     */
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
