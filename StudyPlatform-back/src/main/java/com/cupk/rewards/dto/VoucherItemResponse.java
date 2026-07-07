package com.cupk.rewards.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoucherItemResponse(
        long id,
        String voucherKey,
        String voucherType,
        String name,
        String description,
        int price,
        Integer stockQuantity,
        boolean unlimitedStock,
        String discountType,
        BigDecimal thresholdAmount,
        BigDecimal discountAmount,
        BigDecimal discountRate,
        BigDecimal maxDiscountAmount,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        boolean enabled,
        int sortOrder
) {
}
