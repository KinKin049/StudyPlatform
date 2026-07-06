package com.cupk.auth.repository;

import com.cupk.auth.dto.AuthOnboardingRequest;
import com.cupk.auth.dto.AuthUserResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Persists account credentials and first-run onboarding choices.
 */
@Repository
public class AuthUserRepository {
    private final JdbcTemplate jdbcTemplate;

    public AuthUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByEmail(String email) {
        return count("SELECT COUNT(*) FROM auth_users WHERE email = ?", email) > 0;
    }

    public long insertUser(String username, String email, String passwordHash) {
        String sql = """
                INSERT INTO auth_users (username, email, password_hash, agreement_accepted)
                VALUES (?, ?, ?, 1)
                """;
        jdbcTemplate.update(sql, username, email, passwordHash);
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        long userId = id == null ? 0L : id;
        syncPlatformUserAfterRegistration(userId, username, passwordHash);
        syncProfileAfterRegistration(userId, username);
        return userId;
    }

    public AuthUserRow findByEmail(String email) {
        List<AuthUserRow> rows = jdbcTemplate.query(
                """
                SELECT id, username, email, password_hash, role_type, learning_goal, interests_json,
                       school, teacher_name, pet_key, onboarding_completed
                FROM auth_users
                WHERE email = ?
                LIMIT 1
                """,
                this::mapUserRow,
                email
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public AuthUserResponse findResponseById(long userId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT id, username, email, password_hash, role_type, learning_goal, interests_json,
                       school, teacher_name, pet_key, onboarding_completed
                FROM auth_users
                WHERE id = ?
                LIMIT 1
                """,
                (rs, rowNum) -> mapUserRow(rs, rowNum).toResponse(),
                userId
        );
    }

    public void updatePassword(long userId, String passwordHash) {
        jdbcTemplate.update(
                """
                UPDATE auth_users
                SET password_hash = ?
                WHERE id = ?
                """,
                passwordHash,
                userId
        );
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

    public void updateOnboarding(AuthOnboardingRequest request, String interestsJson) {
        String sql = """
                UPDATE auth_users
                SET role_type = ?, learning_goal = ?, interests_json = CAST(? AS JSON),
                    school = ?, teacher_name = ?, pet_key = ?, onboarding_completed = 1
                WHERE id = ?
                """;
        jdbcTemplate.update(
                sql,
                request.roleType(),
                request.learningGoal(),
                interestsJson,
                request.school(),
                request.teacherName(),
                request.petKey(),
                request.userId()
        );
        syncProfileAfterOnboarding(request);
        syncPlatformUserAfterOnboarding(request);
    }

    private void syncPlatformUserAfterRegistration(long userId, String username, String passwordHash) {
        if (userId <= 0) {
            return;
        }
        String platformUsername = username + "_" + userId;
        jdbcTemplate.update(
                """
                INSERT IGNORE INTO users (id, username, password_hash, nickname, role, enabled)
                VALUES (?, ?, ?, ?, 'STUDENT', 1)
                """,
                userId,
                platformUsername,
                passwordHash,
                username
        );
    }

    private void syncPlatformUserAfterOnboarding(AuthOnboardingRequest request) {
        jdbcTemplate.update(
                """
                UPDATE users
                SET role = ?, nickname = COALESCE(NULLIF(?, ''), nickname)
                WHERE id = ?
                """,
                "teacher".equals(request.roleType()) ? "TEACHER" : "STUDENT",
                "teacher".equals(request.roleType()) ? request.teacherName() : null,
                request.userId()
        );
    }

    private void syncProfileAfterRegistration(long userId, String username) {
        if (userId <= 0) {
            return;
        }
        jdbcTemplate.update(
                """
                INSERT INTO profile_user_profiles
                  (user_id, display_name, handle, role_label, bio, location, school)
                VALUES (?, ?, ?, '学生', '这个账号正在完善自己的学习主页。', 'China', 'StudyPlatform')
                ON DUPLICATE KEY UPDATE
                  display_name = VALUES(display_name),
                  handle = VALUES(handle)
                """,
                userId,
                username,
                "@" + username
        );
    }

    private void syncProfileAfterOnboarding(AuthOnboardingRequest request) {
        String roleLabel = "teacher".equals(request.roleType()) ? "教师" : "学生";
        String school = clean(request.school(), "StudyPlatform");
        String bio = "teacher".equals(request.roleType())
                ? "教师：" + clean(request.teacherName(), "未填写姓名")
                : "目标：" + clean(request.learningGoal(), "持续学习");
        jdbcTemplate.update(
                """
                UPDATE profile_user_profiles
                SET role_label = ?, bio = ?, school = ?
                WHERE user_id = ?
                """,
                roleLabel,
                bio,
                school,
                request.userId()
        );
    }

    private long count(String sql, String value) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, value);
        return count == null ? 0 : count;
    }

    private String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

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
