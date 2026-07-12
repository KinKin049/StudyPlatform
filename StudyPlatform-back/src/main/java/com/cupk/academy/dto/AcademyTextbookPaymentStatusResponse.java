package com.cupk.academy.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 教材支付状态响应DTO，用于返回支付会话的当前状态和订单信息。
 */
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
