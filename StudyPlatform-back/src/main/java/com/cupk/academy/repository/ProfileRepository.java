package com.cupk.academy.repository;

import com.cupk.academy.dto.ProfileLearningEventRequest;
import com.cupk.academy.dto.ProfileUserResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileRepository {

    /**
     * 用户学习档案数据访问层，提供学习事件记录、学习时长统计、用户资料管理和学习数据分析功能。
     */

    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造函数
     *
     * @param jdbcTemplate JDBC模板
     */
    public ProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 插入学习事件记录
     *
     * @param userId 用户ID
     * @param request 学习事件请求
     * @return 记录ID
     */
    public long insertLearningEvent(long userId, ProfileLearningEventRequest request) {
        String sql = """
                INSERT INTO profile_learning_events
                  (user_id, event_type, set_code, question_id, question_type, selected_answer,
                   correct_answer, is_correct, vocabulary_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setString(2, request.eventType());
            ps.setString(3, request.setCode());
            if (request.questionId() == null) {
                ps.setObject(4, null);
            } else {
                ps.setLong(4, request.questionId());
            }
            ps.setString(5, request.questionType());
            ps.setString(6, request.selectedAnswer());
            ps.setString(7, request.correctAnswer());
            ps.setObject(8, request.isCorrect());
            ps.setString(9, request.vocabularyStatus());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    /**
     * 插入学习时长记录
     *
     * @param userId 用户ID
     * @param moduleType 模块类型
     * @param targetCode 目标代码
     * @param targetTitle 目标标题
     * @param durationSeconds 时长（秒）
     * @return 记录ID
     */
    public long insertLearningTimeRecord(
            long userId,
            String moduleType,
            String targetCode,
            String targetTitle,
            int durationSeconds
    ) {
        String sql = """
                INSERT INTO profile_learning_time_records
                  (user_id, module_type, target_code, target_title, duration_seconds)
                VALUES (?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setString(2, moduleType);
            ps.setString(3, targetCode);
            ps.setString(4, targetTitle);
            ps.setInt(5, durationSeconds);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    /**
     * 查询用户档案信息
     *
     * @param userId 用户ID
     * @return 用户档案信息
     */
    public ProfileUserResponse findUserProfile(long userId) {
        ensureUserProfile(userId);
        String sql = """
                SELECT p.user_id, p.display_name, p.handle, p.role_label,
                       COALESCE(u.role_type, CASE WHEN u.role = 'TEACHER' THEN 'teacher' ELSE 'student' END, '') AS role_type,
                       COALESCE(u.teacher_name, '') AS teacher_name,
                       p.bio, p.location, p.school, p.avatar_path
                FROM profile_user_profiles p
                LEFT JOIN users u ON u.id = p.user_id
                WHERE p.user_id = ?
                LIMIT 1
                """;
        return jdbcTemplate.queryForObject(sql, this::mapUserProfile, userId);
    }

    /**
     * 更新用户档案信息
     *
     * @param userId 用户ID
     * @param name 显示名称
     * @param bio 个人简介
     */
    public void updateUserProfile(long userId, String name, String bio) {
        ensureUserProfile(userId);
        String sql = """
                UPDATE profile_user_profiles
                SET display_name = ?, bio = ?
                WHERE user_id = ?
                """;
        jdbcTemplate.update(sql, name, bio, userId);
    }

    /**
     * 更新用户头像路径
     *
     * @param userId 用户ID
     * @param avatarPath 头像路径
     */
    public void updateAvatarPath(long userId, String avatarPath) {
        ensureUserProfile(userId);
        String sql = """
                UPDATE profile_user_profiles
                SET avatar_path = ?
                WHERE user_id = ?
                """;
        jdbcTemplate.update(sql, avatarPath, userId);
    }

    /**
     * 统计用户学习事件总数
     *
     * @param userId 用户ID
     * @return 事件总数
     */
    public long countEvents(long userId) {
        return queryLong("SELECT COUNT(*) FROM profile_learning_events WHERE user_id = ?", userId);
    }

    /**
     * 统计用户正确答题数（按题目去重，取最新一次作答）
     *
     * @param userId 用户ID
     * @return 正确答题数
     */
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

    /**
     * 统计用户已知词汇数（按题目去重，取最新一次学习）
     *
     * @param userId 用户ID
     * @return 已知词汇数
     */
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

    /**
     * 统计用户词汇学习事件数
     *
     * @param userId 用户ID
     * @return 词汇事件数
     */
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

    /**
     * 统计用户练习过的不同题目数
     *
     * @param userId 用户ID
     * @return 练习题目数
     */
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

    /**
     * 统计题库中题目总数
     *
     * @return 题目总数
     */
    public long countTotalQuestions() {
        return queryLong("SELECT COUNT(*) FROM course_question_bank_questions");
    }

    /**
     * 统计用户指定模块的学习时长（秒）
     *
     * @param userId 用户ID
     * @param moduleType 模块类型
     * @return 学习时长（秒）
     */
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

    /**
     * 统计用户所有模块的学习时长（秒）
     *
     * @param userId 用户ID
     * @return 学习时长（秒）
     */
    public long sumAllLearningTimeSeconds(long userId) {
        Long value = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(duration_seconds), 0)
                FROM profile_learning_time_records
                WHERE user_id = ?
                """,
                Long.class,
                userId
        );
        return value == null ? 0 : value;
    }

    /**
     * 统计用户指定时间范围内的学习时长（秒）
     *
     * @param userId 用户ID
     * @param startAt 开始时间
     * @param endAt 结束时间
     * @return 学习时长（秒）
     */
    public long sumLearningTimeSecondsBetween(long userId, LocalDateTime startAt, LocalDateTime endAt) {
        Long value = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(duration_seconds), 0)
                FROM profile_learning_time_records
                WHERE user_id = ? AND created_at >= ? AND created_at < ?
                """,
                Long.class,
                userId,
                startAt,
                endAt
        );
        return value == null ? 0 : value;
    }

    /**
     * 查询用户管理员金币调整值
     *
     * @param userId 用户ID
     * @return 金币调整值
     */
    public long findAdminCoinAdjustment(long userId) {
        ensureUserProfile(userId);
        return queryLong(
                "SELECT COALESCE(admin_coin_adjustment, 0) FROM profile_user_profiles WHERE user_id = ?",
                userId
        );
    }

    /**
     * 查询用户编程题难度分布统计
     *
     * @param userId 用户ID
     * @return 难度分布列表
     */
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

    /**
     * 查询用户题目类型分布统计（选择题、词汇卡片、主观题）
     *
     * @param userId 用户ID
     * @return 类型分布列表
     */
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

    /**
     * 查询用户学习路径统计（按分类）
     *
     * @param userId 用户ID
     * @return 学习路径列表
     */
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

    /**
     * 统计用户指定日期范围内的每日活动数
     *
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 日期到活动数的映射
     */
    public Map<LocalDate, Integer> countEventsByDate(long userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT activity_date, COUNT(*) AS event_count
                FROM (
                  SELECT DATE(created_at) AS activity_date
                  FROM profile_learning_events
                  WHERE user_id = ? AND created_at >= ? AND created_at < ?
                  UNION ALL
                  SELECT DATE(created_at) AS activity_date
                  FROM profile_learning_time_records
                  WHERE user_id = ? AND created_at >= ? AND created_at < ?
                ) activity
                GROUP BY activity_date
                ORDER BY activity_date ASC
                """;
        Map<LocalDate, Integer> counts = new LinkedHashMap<>();
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = endDate.plusDays(1).atStartOfDay();
        jdbcTemplate.query(
                sql,
                rs -> {
                    counts.put(rs.getDate("activity_date").toLocalDate(), rs.getInt("event_count"));
                },
                userId,
                startAt,
                endAt,
                userId,
                startAt,
                endAt
        );
        return counts;
    }

    /**
     * 查询用户活跃日期列表（从指定日期开始）
     *
     * @param userId 用户ID
     * @param startDate 开始日期
     * @return 活跃日期列表
     */
    public List<LocalDate> findActiveDates(long userId, LocalDate startDate) {
        String sql = """
                SELECT DISTINCT activity_date
                FROM (
                  SELECT DATE(created_at) AS activity_date
                  FROM profile_learning_events
                  WHERE user_id = ? AND created_at >= ?
                  UNION ALL
                  SELECT DATE(created_at) AS activity_date
                  FROM profile_learning_time_records
                  WHERE user_id = ? AND created_at >= ?
                ) activity
                ORDER BY activity_date DESC
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getDate("activity_date").toLocalDate(),
                userId,
                startDate.atStartOfDay(),
                userId,
                startDate.atStartOfDay()
        );
    }

    /**
     * 查询用户最近学习事件列表
     *
     * @param userId 用户ID
     * @param limit 返回条数限制
     * @return 最近事件列表
     */
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

    /**
     * 查询长整型值（结果为空时返回0）
     *
     * @param sql SQL语句
     * @param args 参数
     * @return 长整型值
     */
    private long queryLong(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    /**
     * 将结果集映射为类型分布行
     *
     * @param rs 结果集
     * @param rowNum 行号
     * @return 类型分布行
     * @throws SQLException SQL异常
     */
    private DistributionRow mapDistributionRow(ResultSet rs, int rowNum) throws SQLException {
        return new DistributionRow(
                rs.getString("label"),
                rs.getLong("solved_count"),
                rs.getLong("total_count"),
                rs.getString("color")
        );
    }

    /**
     * 将结果集映射为学习路径行
     *
     * @param rs 结果集
     * @param rowNum 行号
     * @return 学习路径行
     * @throws SQLException SQL异常
     */
    private TrackRow mapTrackRow(ResultSet rs, int rowNum) throws SQLException {
        return new TrackRow(
                rs.getString("category_code"),
                rs.getString("category_name"),
                rs.getLong("practiced_count"),
                rs.getLong("total_count")
        );
    }

    /**
     * 将结果集映射为最近事件行
     *
     * @param rs 结果集
     * @param rowNum 行号
     * @return 最近事件行
     * @throws SQLException SQL异常
     */
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

    /**
     * 将结果集映射为用户档案响应对象
     *
     * @param rs 结果集
     * @param rowNum 行号
     * @return 用户档案响应对象
     * @throws SQLException SQL异常
     */
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

    /**
     * 确保用户档案存在（不存在则创建）
     *
     * @param userId 用户ID
     */
    private void ensureUserProfile(long userId) {
        if (queryLong("SELECT COUNT(*) FROM profile_user_profiles WHERE user_id = ?", userId) > 0) {
            return;
        }

        int insertedFromUser = jdbcTemplate.update(
                """
                INSERT INTO profile_user_profiles
                  (user_id, display_name, handle, role_label, bio, location, school)
                SELECT id,
                       COALESCE(NULLIF(nickname, ''), username),
                       CONCAT('@', COALESCE(NULLIF(nickname, ''), username)),
                       CASE WHEN COALESCE(role_type, '') = 'teacher' OR role = 'TEACHER' THEN '教师' ELSE '学生' END,
                       CASE
                         WHEN (COALESCE(role_type, '') = 'teacher' OR role = 'TEACHER') AND teacher_name IS NOT NULL AND teacher_name <> ''
                           THEN CONCAT('教师：', teacher_name)
                         WHEN learning_goal IS NOT NULL AND learning_goal <> ''
                           THEN CONCAT('目标：', learning_goal)
                         ELSE '这个账号正在完善自己的学习主页。'
                       END,
                       'China',
                       COALESCE(NULLIF(school, ''), 'StudyPlatform')
                FROM users
                WHERE id = ?
                """,
                userId
        );
        if (insertedFromUser > 0) {
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

    /**
     * 安全获取布尔值（支持多种数据类型）
     *
     * @param rs 结果集
     * @param columnLabel 列名
     * @return 布尔值，空则返回null
     * @throws SQLException SQL异常
     */
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
