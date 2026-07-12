package com.cupk.academy.dto;

import java.math.BigDecimal;

/**
 * 教材订单响应DTO，用于返回教材订单的创建结果信息。
 */
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
