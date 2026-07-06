package com.cupk.auth.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordResetCodeRepository {
    private final JdbcTemplate jdbcTemplate;

    public PasswordResetCodeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PasswordResetCodeRow findLatest(String email) {
        List<PasswordResetCodeRow> rows = jdbcTemplate.query(
                """
                SELECT id, email, code_hash, expires_at, used, attempt_count, created_at
                FROM password_reset_codes
                WHERE email = ?
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """,
                this::mapRow,
                email
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public PasswordResetCodeRow findLatestUsable(String email, LocalDateTime now) {
        List<PasswordResetCodeRow> rows = jdbcTemplate.query(
                """
                SELECT id, email, code_hash, expires_at, used, attempt_count, created_at
                FROM password_reset_codes
                WHERE email = ?
                  AND used = 0
                  AND expires_at > ?
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """,
                this::mapRow,
                email,
                now
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void markActiveCodesUsed(String email) {
        jdbcTemplate.update(
                """
                UPDATE password_reset_codes
                SET used = 1
                WHERE email = ?
                  AND used = 0
                """,
                email
        );
    }

    public void insert(String email, String codeHash, LocalDateTime expiresAt) {
        jdbcTemplate.update(
                """
                INSERT INTO password_reset_codes (email, code_hash, expires_at)
                VALUES (?, ?, ?)
                """,
                email,
                codeHash,
                expiresAt
        );
    }

    public void incrementAttempt(long id) {
        jdbcTemplate.update(
                """
                UPDATE password_reset_codes
                SET attempt_count = attempt_count + 1
                WHERE id = ?
                """,
                id
        );
    }

    public void markUsed(long id) {
        jdbcTemplate.update(
                """
                UPDATE password_reset_codes
                SET used = 1
                WHERE id = ?
                """,
                id
        );
    }

    private PasswordResetCodeRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PasswordResetCodeRow(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("code_hash"),
                rs.getObject("expires_at", LocalDateTime.class),
                rs.getBoolean("used"),
                rs.getInt("attempt_count"),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }

    public record PasswordResetCodeRow(
            long id,
            String email,
            String codeHash,
            LocalDateTime expiresAt,
            boolean used,
            int attemptCount,
            LocalDateTime createdAt
    ) {
    }
}
