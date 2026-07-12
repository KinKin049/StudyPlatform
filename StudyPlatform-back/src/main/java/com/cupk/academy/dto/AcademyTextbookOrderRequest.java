package com.cupk.academy.dto;

import java.util.List;

/**
 * 教材订单请求DTO，用于接收创建教材订单的请求参数。
 */
public record AcademyTextbookOrderRequest(
        Long userId,
        String textbookId,
        Integer quantity,
        List<Long> cartItemIds,
        Boolean useVoucher,
        String voucherKey
) {
}
