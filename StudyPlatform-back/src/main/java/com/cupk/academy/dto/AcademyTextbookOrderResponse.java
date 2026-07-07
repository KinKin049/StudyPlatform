package com.cupk.academy.dto;

import java.math.BigDecimal;

public record AcademyTextbookOrderResponse(
        String orderNo,
        BigDecimal totalAmount,
        BigDecimal originalAmount,
        BigDecimal discountAmount,
        String voucherKey,
        String voucherName,
        String status,
        String message,
        boolean paid
) {
}
