package com.cupk.academy.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AcademyAssignmentRepository {

    /**
     * 作业数据访问层，提供作业查询、题目管理、答题提交和成绩记录等功能。
     */

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     *
     * @param jdbcTemplate JDBC模板
     * @param objectMapper JSON序列化器
     */
    public AcademyAssignmentRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询作业列表（含用户最新提交状态）
     *
     * @param userId 用户ID
     * @return 作业列表
     */
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

    /**
     * 根据代码查询作业详情
     *
     * @param assignmentCode 作业代码
     * @return 作业详情，不存在则返回空
     */
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

    public long createAssignment(
            String assignmentCode,
            String courseResourceType,
            String courseId,
            String courseTitle,
            String assignmentTitle,
            String teacherName,
            LocalDateTime deadline,
            Integer attemptsLimit,
            Integer durationMinutes,
            Integer totalScore,
            String description
    ) {
        String sql = """
                INSERT INTO academy_assignments
                  (assignment_code, course_resource_type, course_id, course_title,
                   assignment_title, teacher_name, assignment_status, deadline_at,
                   attempts_limit, duration_minutes, total_score, assignment_description)
                VALUES (?, ?, ?, ?, ?, ?, '正在进行', ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, assignmentCode);
            ps.setString(2, courseResourceType);
            ps.setString(3, courseId);
            ps.setString(4, courseTitle);
            ps.setString(5, assignmentTitle);
            ps.setString(6, teacherName);
            ps.setObject(7, deadline);
            ps.setInt(8, attemptsLimit == null || attemptsLimit <= 0 ? 1 : attemptsLimit);
            if (durationMinutes == null || durationMinutes <= 0) {
                ps.setObject(9, null);
            } else {
                ps.setInt(9, durationMinutes);
            }
            ps.setInt(10, totalScore == null || totalScore <= 0 ? 100 : totalScore);
            ps.setString(11, description);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    public void createQuestion(
            Long assignmentId,
            int questionOrder,
            String type,
            String label,
            String title,
            List<String> options,
            String placeholder,
            Integer score,
            Object correctAnswer,
            String explanation,
            boolean autoGradable,
            boolean requiresTeacherReview
    ) {
        String sql = """
                INSERT INTO academy_assignment_questions
                  (assignment_id, question_order, question_type, question_label, question_title,
                   question_options, placeholder_text, score, correct_answer, answer_explanation,
                   auto_gradable, oj_problem_id, requires_teacher_review)
                VALUES (?, ?, ?, ?, ?, CAST(? AS JSON), ?, ?, CAST(? AS JSON), ?, ?, NULL, ?)
                """;
        jdbcTemplate.update(
                sql,
                assignmentId,
                questionOrder,
                type,
                label,
                title,
                writeJson(options == null ? List.of() : options),
                placeholder,
                score == null || score <= 0 ? 1 : score,
                correctAnswer == null ? null : writeJson(correctAnswer),
                explanation,
                autoGradable,
                requiresTeacherReview
        );
    }

    /**
     * 查询作业题目列表
     *
     * @param assignmentId 作业ID
     * @return 题目列表
     */
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

    /**
     * 查询用户最新作业草稿答案
     *
     * @param assignmentId 作业ID
     * @param userId 用户ID
     * @return 草稿答案（JSON反序列化后的Map）
     */
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

    /**
     * 查询用户最新作业提交状态
     *
     * @param assignmentId 作业ID
     * @param userId 用户ID
     * @return 提交状态，不存在则返回空
     */
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

    /**
     * 保存作业草稿
     *
     * @param assignmentId 作业ID
     * @param userId 用户ID
     * @param answers 答案Map
     */
    public void saveDraft(Long assignmentId, Long userId, Map<String, Object> answers) {
        String sql = """
                INSERT INTO academy_assignment_submissions
                  (assignment_id, user_id, submission_status, answer_payload)
                VALUES (?, ?, 'draft', ?)
                """;
        jdbcTemplate.update(sql, assignmentId, userId, writeJson(answers));
    }

    /**
     * 提交作业答案
     *
     * @param assignmentId 作业ID
     * @param userId 用户ID
     * @param answers 答案Map
     * @param score 分数
     * @param pendingTeacherReview 是否待教师审核
     */
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

    /**
     * 创建作业汇总行映射器
     *
     * @return RowMapper
     */
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

    /**
     * 创建作业详情行映射器
     *
     * @return RowMapper
     */
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

    /**
     * 创建作业题目行映射器
     *
     * @return RowMapper
     */
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
