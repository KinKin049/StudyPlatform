package com.cupk.academy.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 教材支付响应DTO，用于返回支付会话创建结果，包含支付二维码信息。
 */
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
