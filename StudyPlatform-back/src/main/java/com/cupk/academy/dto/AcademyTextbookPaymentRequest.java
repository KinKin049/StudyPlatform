package com.cupk.academy.dto;

/**
 * 教材支付请求DTO，用于接收教材订单支付的请求参数。
 */
public record AcademyTextbookPaymentRequest(
        Long userId,
        String provider,
        String paymentMode
) {
}
