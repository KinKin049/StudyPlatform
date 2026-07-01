package com.cupk.welllog.repository;

import com.cupk.welllog.dto.SaveWellLogRecordRequest;
import com.cupk.welllog.model.WellLogRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 测井仿真记录数据访问层。
 * 使用 JdbcTemplate 操作 well_log_record 表，只保存前端提交的参数与报告 JSON。
 */
@Repository
public class WellLogRecordRepository {
    private final JdbcTemplate jdbcTemplate;

    public WellLogRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 插入仿真记录并返回数据库生成的主键。
     */
    public Long create(SaveWellLogRecordRequest request) {
        String sql = """
                INSERT INTO well_log_record (user_id, porosity, oil_saturation, report_json)
                VALUES (?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (request.userId() == null) {
                ps.setObject(1, null);
            } else {
                ps.setLong(1, request.userId());
            }
            ps.setDouble(2, request.porosity());
            ps.setDouble(3, request.oilSaturation());
            ps.setString(4, request.reportJson());
            return ps;
        }, keyHolder);
        return generatedId(keyHolder);
    }

    /**
     * 按用户查询分页记录；userId 为空时只查询匿名记录。
     */
    public List<WellLogRecord> findByUserId(Long userId, int page, int size) {
        int offset = Math.max(page - 1, 0) * size;
        return jdbcTemplate.query("""
                SELECT id, user_id, porosity, oil_saturation, CAST(report_json AS CHAR) AS report_json, create_time
                FROM well_log_record
                WHERE (? IS NULL AND user_id IS NULL) OR user_id = ?
                ORDER BY create_time DESC, id DESC
                LIMIT ? OFFSET ?
                """, mapper(), userId, userId, size, offset);
    }

    /**
     * 统计指定用户的记录数量；userId 为空时统计匿名记录。
     */
    public long countByUserId(Long userId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM well_log_record
                WHERE (? IS NULL AND user_id IS NULL) OR user_id = ?
                """, Long.class, userId, userId);
        return count == null ? 0 : count;
    }

    /**
     * 根据主键查询单条记录。
     */
    public Optional<WellLogRecord> findById(Long id) {
        return jdbcTemplate.query("""
                SELECT id, user_id, porosity, oil_saturation, CAST(report_json AS CHAR) AS report_json, create_time
                FROM well_log_record
                WHERE id = ?
                """, mapper(), id).stream().findFirst();
    }

    /**
     * 根据主键删除记录，返回受影响行数。
     */
    public int deleteById(Long id) {
        return jdbcTemplate.update("DELETE FROM well_log_record WHERE id = ?", id);
    }

    /**
     * 将数据库行映射为测井仿真记录模型。
     */
    private RowMapper<WellLogRecord> mapper() {
        return (rs, rowNum) -> new WellLogRecord(
                rs.getLong("id"),
                getNullableLong(rs, "user_id"),
                rs.getDouble("porosity"),
                rs.getDouble("oil_saturation"),
                rs.getString("report_json"),
                rs.getTimestamp("create_time").toLocalDateTime()
        );
    }

    /**
     * 读取可为空的 BIGINT 字段。
     */
    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * 从 KeyHolder 中提取自增主键，兼容不同 JDBC 驱动返回的数值类型。
     */
    private Long generatedId(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Generated key is missing");
        }
        return key.longValue();
    }
}
