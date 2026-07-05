package com.cupk.academy.repository;

import com.cupk.academy.dto.ProfileLearningEventRequest;
import com.cupk.academy.dto.ProfileUserResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileRepository {
    private final JdbcTemplate jdbcTemplate;

    public ProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertLearningEvent(long userId, ProfileLearningEventRequest request) {
        String sql = """
                INSERT INTO profile_learning_events
                  (user_id, event_type, set_code, question_id, question_type, selected_answer,
                   correct_answer, is_correct, vocabulary_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(
                sql,
                userId,
                request.eventType(),
                request.setCode(),
                request.questionId(),
                request.questionType(),
                request.selectedAnswer(),
                request.correctAnswer(),
                request.isCorrect(),
                request.vocabularyStatus()
        );
    }

    public ProfileUserResponse findUserProfile(long userId) {
        ensureUserProfile(userId);
        String sql = """
                SELECT p.user_id, p.display_name, p.handle, p.role_label,
                       COALESCE(a.role_type, '') AS role_type,
                       COALESCE(a.teacher_name, '') AS teacher_name,
                       p.bio, p.location, p.school, p.avatar_path
                FROM profile_user_profiles p
                LEFT JOIN auth_users a ON a.id = p.user_id
                WHERE p.user_id = ?
                LIMIT 1
                """;
        return jdbcTemplate.queryForObject(sql, this::mapUserProfile, userId);
    }

    public void updateUserProfile(long userId, String name, String bio) {
        ensureUserProfile(userId);
        String sql = """
                UPDATE profile_user_profiles
                SET display_name = ?, bio = ?
                WHERE user_id = ?
                """;
        jdbcTemplate.update(sql, name, bio, userId);
    }

    public void updateAvatarPath(long userId, String avatarPath) {
        ensureUserProfile(userId);
        String sql = """
                UPDATE profile_user_profiles
                SET avatar_path = ?
                WHERE user_id = ?
                """;
        jdbcTemplate.update(sql, avatarPath, userId);
    }

    public long countEvents(long userId) {
        return queryLong("SELECT COUNT(*) FROM profile_learning_events WHERE user_id = ?", userId);
    }

    public long countCorrectAnswers(long userId) {
        return queryLong(
                """
                SELECT COUNT(*)
                FROM profile_learning_events e
                JOIN (
                  SELECT question_id, MAX(id) AS latest_id
                  FROM profile_learning_events
                  WHERE user_id = ? AND event_type = 'answer' AND question_id IS NOT NULL
                  GROUP BY question_id
                ) latest ON latest.latest_id = e.id
                WHERE e.is_correct = 1
                """,
                userId
        );
    }

    public long countKnownVocabulary(long userId) {
        return queryLong(
                """
                SELECT COUNT(*)
                FROM profile_learning_events e
                JOIN (
                  SELECT question_id, MAX(id) AS latest_id
                  FROM profile_learning_events
                  WHERE user_id = ? AND event_type = 'vocabulary' AND question_id IS NOT NULL
                  GROUP BY question_id
                ) latest ON latest.latest_id = e.id
                WHERE e.vocabulary_status = 'known'
                """,
                userId
        );
    }

    public long countVocabularyEvents(long userId) {
        return queryLong(
                """
                SELECT COUNT(*)
                FROM profile_learning_events
                WHERE user_id = ? AND event_type = 'vocabulary'
                """,
                userId
        );
    }

    public long countDistinctPracticedQuestions(long userId) {
        return queryLong(
                """
                SELECT COUNT(DISTINCT question_id)
                FROM profile_learning_events
                WHERE user_id = ? AND question_id IS NOT NULL
                """,
                userId
        );
    }

    public long countTotalQuestions() {
        return queryLong("SELECT COUNT(*) FROM course_question_bank_questions");
    }

    public long sumLearningTimeSeconds(long userId, String moduleType) {
        Long value = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(duration_seconds), 0)
                FROM profile_learning_time_records
                WHERE user_id = ? AND module_type = ?
                """,
                Long.class,
                userId,
                moduleType
        );
        return value == null ? 0 : value;
    }

    public List<CodingDifficultyRow> findCodingDifficultyRows(long userId) {
        String sql = """
                SELECT levels.difficulty,
                       COUNT(DISTINCT attempted.problem_id) AS total_count,
                       COUNT(DISTINCT accepted.problem_id) AS solved_count
                FROM (
                  SELECT 'EASY' AS difficulty
                  UNION ALL SELECT 'MEDIUM'
                  UNION ALL SELECT 'HARD'
                ) levels
                LEFT JOIN oj_problems p
                  ON p.difficulty = levels.difficulty
                 AND p.status = 'PUBLISHED'
                LEFT JOIN oj_submissions attempted
                  ON attempted.problem_id = p.id
                 AND attempted.user_id = ?
                LEFT JOIN oj_submissions accepted
                  ON accepted.problem_id = p.id
                 AND accepted.user_id = ?
                 AND accepted.status = 'ACCEPTED'
                GROUP BY levels.difficulty
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new CodingDifficultyRow(
                rs.getString("difficulty"),
                rs.getLong("solved_count"),
                rs.getLong("total_count")
        ), userId, userId);
    }

    public List<DistributionRow> findDistributionRows(long userId) {
        String sql = """
                SELECT '选择题' AS label, '#2dd4bf' AS color,
                       COUNT(DISTINCT q.id) AS total_count,
                       COUNT(DISTINCT CASE WHEN e.id IS NOT NULL THEN q.id END) AS solved_count
                FROM course_question_bank_questions q
                LEFT JOIN profile_learning_events e
                  ON e.user_id = ? AND e.question_id = q.id AND e.event_type = 'answer'
                WHERE q.question_type IN ('single', 'multiple')
                UNION ALL
                SELECT '词汇卡片' AS label, '#60a5fa' AS color,
                       COUNT(DISTINCT q.id) AS total_count,
                       COUNT(DISTINCT CASE WHEN e.id IS NOT NULL THEN q.id END) AS solved_count
                FROM course_question_bank_questions q
                LEFT JOIN profile_learning_events e
                  ON e.user_id = ? AND e.question_id = q.id AND e.event_type = 'vocabulary'
                WHERE q.question_type = 'vocabulary'
                UNION ALL
                SELECT '主观题' AS label, '#f59e0b' AS color,
                       COUNT(DISTINCT q.id) AS total_count,
                       COUNT(DISTINCT CASE WHEN e.id IS NOT NULL THEN q.id END) AS solved_count
                FROM course_question_bank_questions q
                LEFT JOIN profile_learning_events e
                  ON e.user_id = ? AND e.question_id = q.id AND e.event_type = 'reveal'
                WHERE q.question_type NOT IN ('single', 'multiple', 'vocabulary')
                """;
        return jdbcTemplate.query(sql, this::mapDistributionRow, userId, userId, userId);
    }

    public List<TrackRow> findTrackRows(long userId) {
        String sql = """
                SELECT c.category_code, c.category_name,
                       COUNT(DISTINCT q.id) AS total_count,
                       COUNT(DISTINCT CASE WHEN e.id IS NOT NULL THEN q.id END) AS practiced_count
                FROM course_question_bank_categories c
                LEFT JOIN course_question_bank_sets s ON s.category_id = c.id
                LEFT JOIN course_question_bank_questions q ON q.set_id = s.id
                LEFT JOIN profile_learning_events e ON e.user_id = ? AND e.question_id = q.id
                GROUP BY c.id, c.category_code, c.category_name, c.sort_order
                ORDER BY c.sort_order ASC, c.id ASC
                """;
        return jdbcTemplate.query(sql, this::mapTrackRow, userId);
    }

    public Map<LocalDate, Integer> countEventsByDate(long userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT DATE(created_at) AS activity_date, COUNT(*) AS event_count
                FROM profile_learning_events
                WHERE user_id = ? AND created_at >= ? AND created_at < ?
                GROUP BY DATE(created_at)
                ORDER BY activity_date ASC
                """;
        Map<LocalDate, Integer> counts = new LinkedHashMap<>();
        jdbcTemplate.query(
                sql,
                rs -> {
                    counts.put(rs.getDate("activity_date").toLocalDate(), rs.getInt("event_count"));
                },
                userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );
        return counts;
    }

    public List<LocalDate> findActiveDates(long userId, LocalDate startDate) {
        String sql = """
                SELECT DISTINCT DATE(created_at) AS activity_date
                FROM profile_learning_events
                WHERE user_id = ? AND created_at >= ?
                ORDER BY activity_date DESC
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getDate("activity_date").toLocalDate(),
                userId,
                startDate.atStartOfDay()
        );
    }

    public List<RecentEventRow> findRecentEvents(long userId, int limit) {
        String sql = """
                SELECT e.event_type, e.set_code, e.question_id, e.question_type, e.selected_answer,
                       e.correct_answer, e.is_correct, e.vocabulary_status, e.created_at,
                       COALESCE(qs.title, es.title, e.set_code, '题库') AS set_title,
                       q.stem
                FROM profile_learning_events e
                LEFT JOIN course_question_bank_questions q ON q.id = e.question_id
                LEFT JOIN course_question_bank_sets qs ON qs.id = q.set_id
                LEFT JOIN course_question_bank_sets es ON es.set_code = e.set_code
                WHERE e.user_id = ?
                ORDER BY e.created_at DESC, e.id DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, this::mapRecentEventRow, userId, limit);
    }

    private long queryLong(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private DistributionRow mapDistributionRow(ResultSet rs, int rowNum) throws SQLException {
        return new DistributionRow(
                rs.getString("label"),
                rs.getLong("solved_count"),
                rs.getLong("total_count"),
                rs.getString("color")
        );
    }

    private TrackRow mapTrackRow(ResultSet rs, int rowNum) throws SQLException {
        return new TrackRow(
                rs.getString("category_code"),
                rs.getString("category_name"),
                rs.getLong("practiced_count"),
                rs.getLong("total_count")
        );
    }

    private RecentEventRow mapRecentEventRow(ResultSet rs, int rowNum) throws SQLException {
        return new RecentEventRow(
                rs.getString("event_type"),
                rs.getString("set_code"),
                rs.getObject("question_id", Long.class),
                rs.getString("question_type"),
                rs.getString("selected_answer"),
                rs.getString("correct_answer"),
                getBoolean(rs, "is_correct"),
                rs.getString("vocabulary_status"),
                rs.getString("set_title"),
                rs.getString("stem"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private ProfileUserResponse mapUserProfile(ResultSet rs, int rowNum) throws SQLException {
        String avatarPath = rs.getString("avatar_path");
        String avatarUrl = avatarPath == null || avatarPath.isBlank()
                ? ""
                : "/files/" + avatarPath.replace("\\", "/");
        return new ProfileUserResponse(
                rs.getLong("user_id"),
                rs.getString("display_name"),
                rs.getString("handle"),
                rs.getString("role_label"),
                rs.getString("role_type"),
                rs.getString("teacher_name"),
                rs.getString("bio"),
                rs.getString("location"),
                rs.getString("school"),
                avatarUrl
        );
    }

    private void ensureUserProfile(long userId) {
        if (queryLong("SELECT COUNT(*) FROM profile_user_profiles WHERE user_id = ?", userId) > 0) {
            return;
        }

        int insertedFromAuth = jdbcTemplate.update(
                """
                INSERT INTO profile_user_profiles
                  (user_id, display_name, handle, role_label, bio, location, school)
                SELECT id,
                       username,
                       CONCAT('@', username),
                       CASE WHEN role_type = 'teacher' THEN '教师' ELSE '学生' END,
                       CASE
                         WHEN role_type = 'teacher' AND teacher_name IS NOT NULL AND teacher_name <> ''
                           THEN CONCAT('教师：', teacher_name)
                         WHEN learning_goal IS NOT NULL AND learning_goal <> ''
                           THEN CONCAT('目标：', learning_goal)
                         ELSE '这个账号正在完善自己的学习主页。'
                       END,
                       'China',
                       COALESCE(NULLIF(school, ''), 'StudyPlatform')
                FROM auth_users
                WHERE id = ?
                """,
                userId
        );
        if (insertedFromAuth > 0) {
            return;
        }

        String sql = """
                INSERT INTO profile_user_profiles
                  (user_id, display_name, handle, role_label, bio, location, school)
                VALUES (?, 'Kinkin', '@study-platform', 'StudyPlatform 学习者',
                        '在题库、课程、实验与背单词之间来回穿梭，把零散练习沉淀成稳定的学习曲线。',
                        'China', 'StudyPlatform')
                ON DUPLICATE KEY UPDATE user_id = user_id
                """;
        jdbcTemplate.update(sql, userId);
    }

    private Boolean getBoolean(ResultSet rs, String columnLabel) throws SQLException {
        Object value = rs.getObject(columnLabel);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() != 0;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    public record DistributionRow(
            String label,
            long solved,
            long total,
            String color
    ) {
    }

    public record TrackRow(
            String code,
            String name,
            long practiced,
            long total
    ) {
    }

    public record CodingDifficultyRow(
            String difficulty,
            long solved,
            long total
    ) {
    }

    public record RecentEventRow(
            String eventType,
            String setCode,
            Long questionId,
            String questionType,
            String selectedAnswer,
            String correctAnswer,
            Boolean correct,
            String vocabularyStatus,
            String setTitle,
            String stem,
            LocalDateTime createdAt
    ) {
    }
}
