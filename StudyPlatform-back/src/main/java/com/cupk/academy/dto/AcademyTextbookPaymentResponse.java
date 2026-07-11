package com.cupk.academy.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AcademyTextbookPaymentResponse(
        String sessionId,
        String orderNo,
        String provider,
        BigDecimal amount,
        String qrPayload,
        String status,
        LocalDateTime expiresAt,
        String message
) {
}
