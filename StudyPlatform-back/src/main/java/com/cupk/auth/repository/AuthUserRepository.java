package com.cupk.auth.repository;

import com.cupk.auth.dto.AuthOnboardingRequest;
import com.cupk.auth.dto.AuthUserResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 用户认证数据访问层，持久化账户凭证和首次入职引导信息。
 */
@Repository
public class AuthUserRepository {
    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造函数，注入JdbcTemplate。
     *
     * @param jdbcTemplate 数据库操作模板
     */
    public AuthUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 检查邮箱是否已被注册。
     *
     * @param email 邮箱地址
     * @return 邮箱已存在返回true，否则返回false
     */
    public boolean existsByEmail(String email) {
        return count("SELECT COUNT(*) FROM users WHERE email = ?", email) > 0;
    }

    /**
     * 插入新用户记录。创建用户后同步更新用户名格式和用户资料。
     *
     * @param username 用户名
     * @param email 邮箱地址
     * @param passwordHash 密码哈希值
     * @return 新创建用户的ID
     */
    public long insertUser(String username, String email, String passwordHash) {
        String sql = """
                INSERT INTO users
                  (username, email, password_hash, nickname, role, role_type, enabled,
                   agreement_accepted, onboarding_completed)
                VALUES (?, ?, ?, ?, 'STUDENT', 'student', 1, 1, 0)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, email);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, username);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        long userId = key == null ? 0L : key.longValue();
        if (userId > 0) {
            jdbcTemplate.update("UPDATE users SET username = ? WHERE id = ?", username + "_" + userId, userId);
        }
        syncProfileAfterRegistration(userId, username);
        return userId;
    }

    /**
     * 根据邮箱查询用户记录。
     *
     * @param email 邮箱地址
     * @return 用户行记录，不存在返回null
     */
    public AuthUserRow findByEmail(String email) {
        List<AuthUserRow> rows = jdbcTemplate.query(
                """
                SELECT id, COALESCE(NULLIF(nickname, ''), username) AS username,
                       email, password_hash,
                       COALESCE(NULLIF(role_type, ''),
                         CASE WHEN role = 'TEACHER' THEN 'teacher'
                              WHEN role = 'ADMIN' THEN 'admin'
                              ELSE 'student' END
                       ) AS role_type,
                       learning_goal, interests_json,
                       school, teacher_name, pet_key, onboarding_completed
                FROM users
                WHERE email = ?
                LIMIT 1
                """,
                this::mapUserRow,
                email
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 根据用户ID查询用户记录。
     *
     * @param userId 用户ID
     * @return 用户行记录，不存在返回null
     */
    public AuthUserRow findById(long userId) {
        List<AuthUserRow> rows = jdbcTemplate.query(
                """
                SELECT id, COALESCE(NULLIF(nickname, ''), username) AS username,
                       email, password_hash,
                       COALESCE(NULLIF(role_type, ''),
                         CASE WHEN role = 'TEACHER' THEN 'teacher'
                              WHEN role = 'ADMIN' THEN 'admin'
                              ELSE 'student' END
                       ) AS role_type,
                       learning_goal, interests_json,
                       school, teacher_name, pet_key, onboarding_completed
                FROM users
                WHERE id = ?
                LIMIT 1
                """,
                this::mapUserRow,
                userId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 判断邮箱是否属于其他用户。
     *
     * @param email 邮箱地址
     * @param userId 当前用户ID
     * @return 被其他用户占用返回true
     */
    public boolean emailBelongsToOtherUser(String email, long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ? AND id <> ?",
                Long.class,
                email,
                userId
        );
        return count != null && count > 0;
    }

    /**
     * 根据用户ID查询用户响应对象。
     *
     * @param userId 用户ID
     * @return 用户响应对象
     */
    public AuthUserResponse findResponseById(long userId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT id, COALESCE(NULLIF(nickname, ''), username) AS username,
                       email, password_hash,
                       COALESCE(NULLIF(role_type, ''),
                         CASE WHEN role = 'TEACHER' THEN 'teacher'
                              WHEN role = 'ADMIN' THEN 'admin'
                              ELSE 'student' END
                       ) AS role_type,
                       learning_goal, interests_json,
                       school, teacher_name, pet_key, onboarding_completed
                FROM users
                WHERE id = ?
                LIMIT 1
                """,
                (rs, rowNum) -> mapUserRow(rs, rowNum).toResponse(),
                userId
        );
    }

    /**
     * 更新用户密码。
     *
     * @param userId 用户ID
     * @param passwordHash 新密码哈希值
     */
    public void updatePassword(long userId, String passwordHash) {
        jdbcTemplate.update(
                """
                UPDATE users
                SET password_hash = ?
                WHERE id = ?
                """,
                passwordHash,
                userId
        );
    }

    public void updatePetKey(long userId, String petKey) {
        jdbcTemplate.update(
                """
                UPDATE users
                SET pet_key = ?
                WHERE id = ?
                """,
                petKey,
                userId
        );
    }

    /**
     * 更新用户邮箱。
     *
     * @param userId 用户ID
     * @param email 新邮箱
     */
    public void updateEmail(long userId, String email) {
        jdbcTemplate.update(
                """
                UPDATE users
                SET email = ?
                WHERE id = ?
                """,
                email,
                userId
        );
    }

    /**
     * 更新用户入职引导信息。更新角色类型、学习目标、兴趣等字段，并标记入职完成。
     *
     * @param request 入职请求
     * @param interestsJson 兴趣列表的JSON字符串
     */
    public void updateOnboarding(AuthOnboardingRequest request, String interestsJson) {
        String sql = """
                UPDATE users
                SET role_type = ?,
                    role = CASE WHEN ? = 'teacher' THEN 'TEACHER'
                                WHEN ? = 'admin' THEN 'ADMIN'
                                ELSE 'STUDENT' END,
                    learning_goal = ?, interests_json = CAST(? AS JSON),
                    school = ?, teacher_name = ?, pet_key = ?, onboarding_completed = 1,
                    nickname = COALESCE(NULLIF(?, ''), nickname)
                WHERE id = ?
                """;
        jdbcTemplate.update(
                sql,
                request.roleType(),
                request.roleType(),
                request.roleType(),
                request.learningGoal(),
                interestsJson,
                request.school(),
                request.teacherName(),
                request.petKey(),
                "teacher".equals(request.roleType()) ? request.teacherName() : null,
                request.userId()
        );
        syncProfileAfterOnboarding(request);
    }

    /**
     * 用户注册后同步用户资料。创建或更新用户资料记录。
     *
     * @param userId 用户ID
     * @param username 用户名
     */
    private void syncProfileAfterRegistration(long userId, String username) {
        if (userId <= 0) {
            return;
        }
        jdbcTemplate.update(
                """
                INSERT INTO profile_user_profiles
                  (user_id, display_name, handle, role_label, bio, location, profile_tags_json, school)
                VALUES (?, ?, ?, '学生', '这个账号正在完善自己的学习主页。', 'China',
                        JSON_ARRAY('目标：稳稳变强'), 'StudyPlatform')
                ON DUPLICATE KEY UPDATE
                  display_name = VALUES(display_name),
                  handle = VALUES(handle)
                """,
                userId,
                username,
                "@" + username
        );
    }

    /**
     * 入职引导完成后同步用户资料。更新角色标签、个人简介和学校信息。
     *
     * @param request 入职请求
     */
    private void syncProfileAfterOnboarding(AuthOnboardingRequest request) {
        String roleLabel = "teacher".equals(request.roleType()) ? "教师" : "学生";
        String school = clean(request.school(), "StudyPlatform");
        String bio = "teacher".equals(request.roleType())
                ? "教师：" + clean(request.teacherName(), "未填写姓名")
                : "目标：" + clean(request.learningGoal(), "持续学习");
        jdbcTemplate.update(
                """
                UPDATE profile_user_profiles
                SET role_label = ?, bio = ?, profile_tags_json = JSON_ARRAY(?), school = ?
                WHERE user_id = ?
                """,
                roleLabel,
                bio,
                "目标：" + clean(request.learningGoal(), "持续学习"),
                school,
                request.userId()
        );
    }

    /**
     * 执行计数查询。
     *
     * @param sql SQL查询语句
     * @param value 查询参数值
     * @return 计数结果，空值返回0
     */
    private long count(String sql, String value) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, value);
        return count == null ? 0 : count;
    }

    /**
     * 清理字符串，空值返回默认值。
     *
     * @param value 待清理的字符串
     * @param fallback 默认值
     * @return 清理后的字符串
     */
    private String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    /**
     * 将ResultSet映射为AuthUserRow对象。
     *
     * @param rs 结果集
     * @param rowNum 行号
     * @return AuthUserRow对象
     * @throws SQLException SQL异常
     */
    private AuthUserRow mapUserRow(ResultSet rs, int rowNum) throws SQLException {
        return new AuthUserRow(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("role_type"),
                rs.getString("learning_goal"),
                parseJsonArray(rs.getString("interests_json")),
                rs.getString("school"),
                rs.getString("teacher_name"),
                rs.getString("pet_key"),
                rs.getBoolean("onboarding_completed")
        );
    }

    /**
     * 解析JSON数组字符串为字符串列表。
     *
     * @param json JSON数组字符串
     * @return 字符串列表
     */
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank() || "null".equalsIgnoreCase(json)) {
            return List.of();
        }
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return List.of();
        }
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : body.split(",")) {
            String value = part.trim();
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                value = value.substring(1, value.length() - 1);
            }
            value = value.replace("\\\"", "\"").replace("\\\\", "\\");
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    /**
     * 用户行记录，映射数据库users表的查询结果。
     *
     * @param id 用户ID
     * @param username 用户名
     * @param email 邮箱地址
     * @param passwordHash 密码哈希值
     * @param roleType 角色类型
     * @param learningGoal 学习目标
     * @param interests 兴趣列表
     * @param school 学校
     * @param teacherName 教师姓名
     * @param petKey 宠物标识
     * @param onboardingCompleted 是否完成入职引导
     */
    public record AuthUserRow(
            Long id,
            String username,
            String email,
            String passwordHash,
            String roleType,
            String learningGoal,
            List<String> interests,
            String school,
            String teacherName,
            String petKey,
            Boolean onboardingCompleted
    ) {
        /**
         * 转换为AuthUserResponse对象。
         *
         * @return 用户响应对象
         */
        public AuthUserResponse toResponse() {
            return new AuthUserResponse(
                    id,
                    username,
                    email,
                    roleType,
                    learningGoal,
                    interests,
                    school,
                    teacherName,
                    petKey,
                    onboardingCompleted
            );
        }
    }
}
