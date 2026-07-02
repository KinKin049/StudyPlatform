package com.cupk.production.repository;

import com.cupk.production.dto.SavePumpRecordRequest;
import com.cupk.production.dto.SaveReservoirRecordRequest;
import com.cupk.production.dto.SaveStimulationRecordRequest;
import com.cupk.production.dto.SaveWaterfloodRecordRequest;
import com.cupk.production.model.ProductionPumpRecord;
import com.cupk.production.model.ProductionReservoirRecord;
import com.cupk.production.model.ProductionStimulationRecord;
import com.cupk.production.model.ProductionWaterfloodRecord;
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
 * 采油生产仿真记录数据访问层。
 * 仓储层只处理前端计算结果的持久化，不包含任何仿真公式。
 */
@Repository
public class ProductionRecordRepository {
    private final JdbcTemplate jdbcTemplate;

    public ProductionRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long createPumpRecord(SavePumpRecordRequest request) {
        return insert("""
                INSERT INTO production_pump_record
                    (user_id, stroke, stroke_times, pump_diameter, work_condition, indicator_chart_data)
                VALUES (?, ?, ?, ?, ?, ?)
                """, ps -> {
            setNullableLong(ps, 1, request.userId());
            ps.setDouble(2, request.stroke());
            ps.setDouble(3, request.strokeTimes());
            ps.setDouble(4, request.pumpDiameter());
            ps.setString(5, request.workCondition());
            ps.setString(6, request.indicatorChartData());
        });
    }

    public Long createReservoirRecord(SaveReservoirRecordRequest request) {
        return insert("""
                INSERT INTO production_reservoir_record
                    (user_id, formation_pressure, permeability, water_saturation, viscosity, daily_oil, daily_water)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, ps -> {
            setNullableLong(ps, 1, request.userId());
            ps.setDouble(2, request.formationPressure());
            ps.setDouble(3, request.permeability());
            ps.setDouble(4, request.waterSaturation());
            ps.setDouble(5, request.viscosity());
            ps.setDouble(6, request.dailyOil());
            ps.setDouble(7, request.dailyWater());
        });
    }

    public Long createWaterfloodRecord(SaveWaterfloodRecordRequest request) {
        return insert("""
                INSERT INTO production_waterflood_record
                    (user_id, injection_rate, effect_day, water_breakthrough_day, peak_oil, production_curve)
                VALUES (?, ?, ?, ?, ?, ?)
                """, ps -> {
            setNullableLong(ps, 1, request.userId());
            ps.setDouble(2, request.injectionRate());
            ps.setInt(3, request.effectDay());
            ps.setInt(4, request.waterBreakthroughDay());
            ps.setDouble(5, request.peakOil());
            ps.setString(6, request.productionCurve());
        });
    }

    public Long createStimulationRecord(SaveStimulationRecordRequest request) {
        return insert("""
                INSERT INTO production_stimulation_record
                    (user_id, type, sand_volume, displacement, acid_volume, fracture_length, stimulation_ratio)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, ps -> {
            setNullableLong(ps, 1, request.userId());
            ps.setString(2, request.type());
            ps.setObject(3, request.sandVolume());
            ps.setDouble(4, request.displacement());
            ps.setObject(5, request.acidVolume());
            ps.setDouble(6, request.fractureLength());
            ps.setDouble(7, request.stimulationRatio());
        });
    }

    public List<ProductionPumpRecord> findPumpRecords(Long userId, int page, int size) {
        return jdbcTemplate.query("""
                SELECT id, user_id, stroke, stroke_times, pump_diameter, work_condition,
                       CAST(indicator_chart_data AS CHAR) AS indicator_chart_data, create_time
                FROM production_pump_record
                WHERE (? IS NULL AND user_id IS NULL) OR user_id = ?
                ORDER BY create_time DESC, id DESC
                LIMIT ? OFFSET ?
                """, pumpMapper(), userId, userId, size, offset(page, size));
    }

    public List<ProductionReservoirRecord> findReservoirRecords(Long userId, int page, int size) {
        return jdbcTemplate.query("""
                SELECT id, user_id, formation_pressure, permeability, water_saturation,
                       viscosity, daily_oil, daily_water, create_time
                FROM production_reservoir_record
                WHERE (? IS NULL AND user_id IS NULL) OR user_id = ?
                ORDER BY create_time DESC, id DESC
                LIMIT ? OFFSET ?
                """, reservoirMapper(), userId, userId, size, offset(page, size));
    }

    public List<ProductionWaterfloodRecord> findWaterfloodRecords(Long userId, int page, int size) {
        return jdbcTemplate.query("""
                SELECT id, user_id, injection_rate, effect_day, water_breakthrough_day,
                       peak_oil, CAST(production_curve AS CHAR) AS production_curve, create_time
                FROM production_waterflood_record
                WHERE (? IS NULL AND user_id IS NULL) OR user_id = ?
                ORDER BY create_time DESC, id DESC
                LIMIT ? OFFSET ?
                """, waterfloodMapper(), userId, userId, size, offset(page, size));
    }

    public List<ProductionStimulationRecord> findStimulationRecords(Long userId, int page, int size) {
        return jdbcTemplate.query("""
                SELECT id, user_id, type, sand_volume, displacement, acid_volume,
                       fracture_length, stimulation_ratio, create_time
                FROM production_stimulation_record
                WHERE (? IS NULL AND user_id IS NULL) OR user_id = ?
                ORDER BY create_time DESC, id DESC
                LIMIT ? OFFSET ?
                """, stimulationMapper(), userId, userId, size, offset(page, size));
    }

    public long count(String tableName, Long userId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM %s
                WHERE (? IS NULL AND user_id IS NULL) OR user_id = ?
                """.formatted(tableName), Long.class, userId, userId);
        return count == null ? 0 : count;
    }

    public Optional<ProductionPumpRecord> findPumpById(Long id) {
        return jdbcTemplate.query("""
                SELECT id, user_id, stroke, stroke_times, pump_diameter, work_condition,
                       CAST(indicator_chart_data AS CHAR) AS indicator_chart_data, create_time
                FROM production_pump_record
                WHERE id = ?
                """, pumpMapper(), id).stream().findFirst();
    }

    public Optional<ProductionReservoirRecord> findReservoirById(Long id) {
        return jdbcTemplate.query("""
                SELECT id, user_id, formation_pressure, permeability, water_saturation,
                       viscosity, daily_oil, daily_water, create_time
                FROM production_reservoir_record
                WHERE id = ?
                """, reservoirMapper(), id).stream().findFirst();
    }

    public Optional<ProductionWaterfloodRecord> findWaterfloodById(Long id) {
        return jdbcTemplate.query("""
                SELECT id, user_id, injection_rate, effect_day, water_breakthrough_day,
                       peak_oil, CAST(production_curve AS CHAR) AS production_curve, create_time
                FROM production_waterflood_record
                WHERE id = ?
                """, waterfloodMapper(), id).stream().findFirst();
    }

    public Optional<ProductionStimulationRecord> findStimulationById(Long id) {
        return jdbcTemplate.query("""
                SELECT id, user_id, type, sand_volume, displacement, acid_volume,
                       fracture_length, stimulation_ratio, create_time
                FROM production_stimulation_record
                WHERE id = ?
                """, stimulationMapper(), id).stream().findFirst();
    }

    public int deleteById(String tableName, Long id) {
        return jdbcTemplate.update("DELETE FROM " + tableName + " WHERE id = ?", id);
    }

    private Long insert(String sql, StatementBinder binder) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            binder.bind(ps);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Generated key is missing");
        }
        return key.longValue();
    }

    private RowMapper<ProductionPumpRecord> pumpMapper() {
        return (rs, rowNum) -> new ProductionPumpRecord(
                rs.getLong("id"),
                getNullableLong(rs, "user_id"),
                rs.getDouble("stroke"),
                rs.getDouble("stroke_times"),
                rs.getDouble("pump_diameter"),
                rs.getString("work_condition"),
                rs.getString("indicator_chart_data"),
                rs.getTimestamp("create_time").toLocalDateTime()
        );
    }

    private RowMapper<ProductionReservoirRecord> reservoirMapper() {
        return (rs, rowNum) -> new ProductionReservoirRecord(
                rs.getLong("id"),
                getNullableLong(rs, "user_id"),
                rs.getDouble("formation_pressure"),
                rs.getDouble("permeability"),
                rs.getDouble("water_saturation"),
                rs.getDouble("viscosity"),
                rs.getDouble("daily_oil"),
                rs.getDouble("daily_water"),
                rs.getTimestamp("create_time").toLocalDateTime()
        );
    }

    private RowMapper<ProductionWaterfloodRecord> waterfloodMapper() {
        return (rs, rowNum) -> new ProductionWaterfloodRecord(
                rs.getLong("id"),
                getNullableLong(rs, "user_id"),
                rs.getDouble("injection_rate"),
                rs.getInt("effect_day"),
                rs.getInt("water_breakthrough_day"),
                rs.getDouble("peak_oil"),
                rs.getString("production_curve"),
                rs.getTimestamp("create_time").toLocalDateTime()
        );
    }

    private RowMapper<ProductionStimulationRecord> stimulationMapper() {
        return (rs, rowNum) -> new ProductionStimulationRecord(
                rs.getLong("id"),
                getNullableLong(rs, "user_id"),
                rs.getString("type"),
                getNullableDouble(rs, "sand_volume"),
                rs.getDouble("displacement"),
                getNullableDouble(rs, "acid_volume"),
                rs.getDouble("fracture_length"),
                rs.getDouble("stimulation_ratio"),
                rs.getTimestamp("create_time").toLocalDateTime()
        );
    }

    private int offset(int page, int size) {
        return Math.max(page - 1, 0) * size;
    }

    private void setNullableLong(java.sql.PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setObject(index, null);
            return;
        }
        ps.setLong(index, value);
    }

    private Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(java.sql.PreparedStatement ps) throws SQLException;
    }
}
