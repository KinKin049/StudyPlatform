package com.cupk.academy.dto;

public record AcademyTextbookPaymentRequest(
        Long userId,
        String provider,
        String paymentMode
) {
}
