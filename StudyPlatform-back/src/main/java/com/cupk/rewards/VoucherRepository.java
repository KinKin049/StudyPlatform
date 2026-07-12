package com.cupk.rewards;

import com.cupk.rewards.dto.UserVoucherResponse;
import com.cupk.rewards.dto.VoucherItemResponse;
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 卡券数据访问层。
 * 提供用户卡券查询、兑换、使用及库存管理等数据操作。
 */
@Repository
public class VoucherRepository {
    private final JdbcTemplate jdbcTemplate;

    public VoucherRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UserVoucherResponse> findUserVouchers(long userId) {
        return jdbcTemplate.query(
                """
                SELECT v.id, v.voucher_key, v.voucher_type, v.name, v.description, v.quantity, v.updated_at,
                       i.discount_type, i.threshold_amount, i.discount_amount, i.discount_rate,
                       i.max_discount_amount, i.valid_from, i.valid_until
                FROM user_vouchers v
                LEFT JOIN voucher_items i ON i.voucher_key = v.voucher_key
                WHERE v.user_id = ? AND v.quantity > 0
                ORDER BY v.updated_at DESC, v.id DESC
                """,
                this::mapVoucher,
                userId
        );
    }

    public List<VoucherItemResponse> findAvailableItems() {
        return jdbcTemplate.query(
                """
                SELECT id, voucher_key, voucher_type, name, description, price, stock_quantity, unlimited_stock,
                       discount_type, threshold_amount, discount_amount, discount_rate, max_discount_amount,
                       valid_from, valid_until, enabled, sort_order
                FROM voucher_items
                WHERE enabled = 1
                  AND (valid_from IS NULL OR valid_from <= CURRENT_TIMESTAMP)
                  AND (valid_until IS NULL OR valid_until >= CURRENT_TIMESTAMP)
                  AND (unlimited_stock = 1 OR COALESCE(stock_quantity, 0) > 0)
                ORDER BY voucher_type ASC, sort_order ASC, id ASC
                """,
                this::mapItem
        );
    }

    public VoucherItemResponse findAvailableItem(String voucherKey) {
        return jdbcTemplate.query(
                """
                SELECT id, voucher_key, voucher_type, name, description, price, stock_quantity, unlimited_stock,
                       discount_type, threshold_amount, discount_amount, discount_rate, max_discount_amount,
                       valid_from, valid_until, enabled, sort_order
                FROM voucher_items
                WHERE voucher_key = ?
                  AND enabled = 1
                  AND (valid_from IS NULL OR valid_from <= CURRENT_TIMESTAMP)
                  AND (valid_until IS NULL OR valid_until >= CURRENT_TIMESTAMP)
                LIMIT 1
                """,
                this::mapItem,
                voucherKey
        ).stream().findFirst().orElse(null);
    }

    public VoucherItemResponse findItem(String voucherKey) {
        return jdbcTemplate.query(
                """
                SELECT id, voucher_key, voucher_type, name, description, price, stock_quantity, unlimited_stock,
                       discount_type, threshold_amount, discount_amount, discount_rate, max_discount_amount,
                       valid_from, valid_until, enabled, sort_order
                FROM voucher_items
                WHERE voucher_key = ?
                LIMIT 1
                """,
                this::mapItem,
                voucherKey
        ).stream().findFirst().orElse(null);
    }

    public void addVoucher(long userId, VoucherItemResponse item, int quantity) {
        jdbcTemplate.update(
                """
                INSERT INTO user_vouchers
                  (user_id, voucher_key, voucher_type, name, description, quantity)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  voucher_type = VALUES(voucher_type),
                  name = VALUES(name),
                  description = VALUES(description),
                  quantity = quantity + VALUES(quantity)
                """,
                userId,
                item.voucherKey(),
                item.voucherType(),
                item.name(),
                item.description(),
                quantity
        );
    }

    public boolean decreasePlatformStock(String voucherKey) {
        return jdbcTemplate.update(
                """
                UPDATE voucher_items
                SET stock_quantity = CASE
                    WHEN unlimited_stock = 1 THEN stock_quantity
                    ELSE stock_quantity - 1
                  END
                WHERE voucher_key = ?
                  AND enabled = 1
                  AND (valid_from IS NULL OR valid_from <= CURRENT_TIMESTAMP)
                  AND (valid_until IS NULL OR valid_until >= CURRENT_TIMESTAMP)
                  AND (unlimited_stock = 1 OR COALESCE(stock_quantity, 0) > 0)
                """,
                voucherKey
        ) > 0;
    }

    public boolean useVoucher(long userId, String voucherKey) {
        return jdbcTemplate.update(
                """
                UPDATE user_vouchers
                SET quantity = quantity - 1
                WHERE user_id = ? AND voucher_key = ? AND quantity > 0
                """,
                userId,
                voucherKey
        ) > 0;
    }

    public int findQuantity(long userId, String voucherKey) {
        Integer value = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(MAX(quantity), 0)
                FROM user_vouchers
                WHERE user_id = ? AND voucher_key = ?
                """,
                Integer.class,
                userId,
                voucherKey
        );
        return value == null ? 0 : value;
    }

    public long findAdminCoinAdjustment(long userId) {
        Long value = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(admin_coin_adjustment, 0)
                FROM profile_user_profiles
                WHERE user_id = ?
                """,
                Long.class,
                userId
        );
        return value == null ? 0L : value;
    }

    private UserVoucherResponse mapVoucher(ResultSet rs, int rowNum) throws SQLException {
        return new UserVoucherResponse(
                rs.getLong("id"),
                rs.getString("voucher_key"),
                rs.getString("voucher_type"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("quantity"),
                rs.getString("discount_type"),
                rs.getBigDecimal("threshold_amount"),
                rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("discount_rate"),
                rs.getBigDecimal("max_discount_amount"),
                toLocalDateTime(rs.getTimestamp("valid_from")),
                toLocalDateTime(rs.getTimestamp("valid_until")),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    private VoucherItemResponse mapItem(ResultSet rs, int rowNum) throws SQLException {
        return new VoucherItemResponse(
                rs.getLong("id"),
                rs.getString("voucher_key"),
                rs.getString("voucher_type"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("price"),
                (Integer) rs.getObject("stock_quantity"),
                rs.getBoolean("unlimited_stock"),
                rs.getString("discount_type"),
                rs.getBigDecimal("threshold_amount"),
                rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("discount_rate"),
                rs.getBigDecimal("max_discount_amount"),
                toLocalDateTime(rs.getTimestamp("valid_from")),
                toLocalDateTime(rs.getTimestamp("valid_until")),
                rs.getBoolean("enabled"),
                rs.getInt("sort_order")
        );
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
