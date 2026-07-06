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
 * Persists account credentials and first-run onboarding choices.
 */
@Repository
public class AuthUserRepository {
    private final JdbcTemplate jdbcTemplate;

    public AuthUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByEmail(String email) {
        return count("SELECT COUNT(*) FROM users WHERE email = ?", email) > 0;
    }

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
