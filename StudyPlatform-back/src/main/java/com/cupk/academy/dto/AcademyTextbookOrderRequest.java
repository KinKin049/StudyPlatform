package com.cupk.academy.dto;

import java.util.List;

public record AcademyTextbookOrderRequest(
        Long userId,
        String textbookId,
        Integer quantity,
        List<Long> cartItemIds,
        Boolean useVoucher,
        String voucherKey
) {
}
