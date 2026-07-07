package com.cupk.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminVoucherItemRequest(
        String voucherKey,
        String voucherType,
        String name,
        String description,
        Integer price,
        Integer stockQuantity,
        Boolean unlimitedStock,
        String discountType,
        BigDecimal thresholdAmount,
        BigDecimal discountAmount,
        BigDecimal discountRate,
        BigDecimal maxDiscountAmount,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        Boolean enabled,
        Integer sortOrder
) {
}
