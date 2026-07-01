package com.cupk.welllog.repository;

import com.cupk.welllog.model.WellLogTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * 测井模板数据访问层。
 * 负责读取 well_log_template 表中的深度序列和基础 GR JSON 字段。
 */
@Repository
public class WellLogTemplateRepository {
    private final JdbcTemplate jdbcTemplate;

    public WellLogTemplateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询所有模板，JSON 字段转换为字符串返回给前端。
     */
    public List<WellLogTemplate> findAll() {
        return jdbcTemplate.query("""
                SELECT id, template_name, CAST(depth_array AS CHAR) AS depth_array,
                       CAST(gr_base AS CHAR) AS gr_base, remark, create_time, update_time
                FROM well_log_template
                ORDER BY id DESC
                """, mapper());
    }

    /**
     * 根据主键查询模板详情。
     */
    public Optional<WellLogTemplate> findById(Long id) {
        return jdbcTemplate.query("""
                SELECT id, template_name, CAST(depth_array AS CHAR) AS depth_array,
                       CAST(gr_base AS CHAR) AS gr_base, remark, create_time, update_time
                FROM well_log_template
                WHERE id = ?
                """, mapper(), id).stream().findFirst();
    }

    /**
     * 将模板数据行映射为领域模型。
     */
    private RowMapper<WellLogTemplate> mapper() {
        return (rs, rowNum) -> new WellLogTemplate(
                rs.getLong("id"),
                rs.getString("template_name"),
                rs.getString("depth_array"),
                rs.getString("gr_base"),
                rs.getString("remark"),
                rs.getTimestamp("create_time").toLocalDateTime(),
                rs.getTimestamp("update_time").toLocalDateTime()
        );
    }
}
