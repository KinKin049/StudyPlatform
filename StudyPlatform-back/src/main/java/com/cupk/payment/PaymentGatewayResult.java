package com.cupk.payment;

/**
 * 支付网关操作结果，封装支付创建和查询的返回信息。
 */
public record PaymentGatewayResult(
        String sessionId,
        String provider,
        String qrPayload,
        String status,
        String tradeNo,
        String message
) {
}
