package com.cupk.payment;

public record PaymentGatewayResult(
        String sessionId,
        String provider,
        String qrPayload,
        String status,
        String tradeNo,
        String message
) {
}
