package com.cupk.academy.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 教材购物车商品响应DTO，用于返回购物车中教材商品的详细信息。
 */
public record AcademyTextbookCartItemResponse(
        Long id,
        String textbookId,
        String name,
        String editor,
        String publisher,
        String cover,
        String coverUrl,
        BigDecimal unitPrice,
        Integer quantity,
        LocalDateTime createdAt
) {
}
