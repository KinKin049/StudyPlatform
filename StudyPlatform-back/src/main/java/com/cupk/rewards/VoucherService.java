package com.cupk.rewards;

import com.cupk.rewards.dto.UserVoucherResponse;
import com.cupk.rewards.dto.VoucherItemResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VoucherService {
    private final VoucherRepository voucherRepository;
    private final CoinRewardService coinRewardService;

    public VoucherService(VoucherRepository voucherRepository, CoinRewardService coinRewardService) {
        this.voucherRepository = voucherRepository;
        this.coinRewardService = coinRewardService;
    }

    public List<UserVoucherResponse> findUserVouchers(long userId) {
        return voucherRepository.findUserVouchers(userId);
    }

    public List<VoucherItemResponse> findAvailableItems() {
        return voucherRepository.findAvailableItems();
    }

    @Transactional
    public List<UserVoucherResponse> exchange(long userId, String voucherKey) {
        String normalizedKey = VoucherCatalog.normalize(voucherKey);
        VoucherItemResponse item = voucherRepository.findAvailableItem(normalizedKey);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "卡券未上架或库存不足");
        }
        long balance = coinRewardService.totalCoins(userId) + voucherRepository.findAdminCoinAdjustment(userId);
        if (balance < item.price()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "金币不足");
        }
        if (!voucherRepository.decreasePlatformStock(item.voucherKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "卡券库存不足");
        }
        voucherRepository.addVoucher(userId, item, 1);
        coinRewardService.spendCoins(
                userId,
                "voucher_exchange",
                "voucher-exchange:" + item.voucherKey() + ":" + System.nanoTime(),
                "兑换" + item.name(),
                item.price(),
                null
        );
        return findUserVouchers(userId);
    }

    /**
     * 使用卡券。
     * @param userId 用户ID
     * @param voucherKey 卡券标识
     * @return 使用后用户的卡券列表
     */
    @Transactional
    public List<UserVoucherResponse> use(long userId, String voucherKey) {
        VoucherCatalog.find(voucherKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "未知卡券"));
        if (!voucherRepository.useVoucher(userId, VoucherCatalog.normalize(voucherKey))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "卡券数量不足");
        }
        return findUserVouchers(userId);
    }

    /**
     * 检查用户是否拥有指定卡券。
     * @param userId 用户ID
     * @param voucherKey 卡券标识
     * @return 拥有返回true，否则返回false
     */
    public boolean hasVoucher(long userId, String voucherKey) {
        String normalizedKey = VoucherCatalog.normalize(voucherKey);
        return voucherRepository.findQuantity(userId, normalizedKey) > 0;
    }

    public void consumeDiscountVoucher(long userId, String voucherKey) {
        String normalizedKey = VoucherCatalog.normalize(voucherKey);
        VoucherItemResponse item = voucherRepository.findAvailableItem(normalizedKey);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "优惠券已失效");
        }
        if (!"DISCOUNT".equals(item.voucherType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该卡券不能用于教材优惠");
        }
        if (!voucherRepository.useVoucher(userId, normalizedKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "优惠券数量不足");
        }
    }

    /**
     * 查询有效的优惠券。
     * @param voucherKey 优惠券标识
     * @return 优惠券详情
     */
    public VoucherItemResponse findDiscountVoucher(String voucherKey) {
        String normalizedKey = VoucherCatalog.normalize(voucherKey);
        VoucherItemResponse item = voucherRepository.findAvailableItem(normalizedKey);
        if (item == null || !"DISCOUNT".equals(item.voucherType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "优惠券已失效");
        }
        return item;
    }

    public BigDecimal calculateDiscount(VoucherItemResponse item, BigDecimal originalAmount) {
        BigDecimal safeOriginal = originalAmount == null ? BigDecimal.ZERO : originalAmount.max(BigDecimal.ZERO);
        BigDecimal threshold = item.thresholdAmount() == null ? BigDecimal.ZERO : item.thresholdAmount();
        if (safeOriginal.compareTo(threshold) < 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount = switch (item.discountType() == null ? "" : item.discountType()) {
            case "AMOUNT" -> item.discountAmount() == null ? BigDecimal.ZERO : item.discountAmount();
            case "PERCENT" -> calculatePercentDiscount(item, safeOriginal);
            default -> BigDecimal.ZERO;
        };
        BigDecimal maxDiscount = item.maxDiscountAmount();
        if (maxDiscount != null && maxDiscount.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }
        return discount.max(BigDecimal.ZERO).min(safeOriginal).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算百分比折扣金额。
     * @param item 优惠券详情
     * @param originalAmount 原始金额
     * @return 折扣金额
     */
    private BigDecimal calculatePercentDiscount(VoucherItemResponse item, BigDecimal originalAmount) {
        if (item.discountRate() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal payableRate = item.discountRate();
        BigDecimal discountRate = BigDecimal.ONE.subtract(payableRate);
        if (discountRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return originalAmount.multiply(discountRate);
    }
}
