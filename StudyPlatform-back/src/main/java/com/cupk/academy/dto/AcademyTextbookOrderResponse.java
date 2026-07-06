package com.cupk.academy.dto;

import java.math.BigDecimal;

public record AcademyTextbookOrderResponse(
        String orderNo,
        BigDecimal totalAmount,
        String status,
        String message,
        boolean paid
) {
}
