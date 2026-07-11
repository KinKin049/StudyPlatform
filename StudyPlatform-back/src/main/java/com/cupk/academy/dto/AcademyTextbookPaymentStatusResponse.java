package com.cupk.academy.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AcademyTextbookPaymentStatusResponse(
        String sessionId,
        String orderNo,
        String provider,
        BigDecimal amount,
        String status,
        boolean paid,
        LocalDateTime expiresAt,
        AcademyTextbookOrderResponse order,
        String message
) {
}
