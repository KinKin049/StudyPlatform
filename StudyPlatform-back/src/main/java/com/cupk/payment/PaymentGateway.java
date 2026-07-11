package com.cupk.payment;

import java.math.BigDecimal;

public interface PaymentGateway {
    String provider();

    PaymentGatewayResult createNativePayment(String orderNo, String subject, BigDecimal amount);

    default PaymentGatewayResult createPagePayment(String orderNo, String subject, BigDecimal amount, String returnUrl) {
        throw new UnsupportedOperationException("Page payment is not supported");
    }

    PaymentGatewayResult queryPayment(String orderNo);
}
