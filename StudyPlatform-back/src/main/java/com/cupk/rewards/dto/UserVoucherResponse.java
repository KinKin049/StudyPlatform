package com.cupk.rewards.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserVoucherResponse(
        long id,
        String voucherKey,
        String voucherType,
        String name,
        String description,
        int quantity,
        String discountType,
        BigDecimal thresholdAmount,
        BigDecimal discountAmount,
        BigDecimal discountRate,
        BigDecimal maxDiscountAmount,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        LocalDateTime updatedAt
) {
}
