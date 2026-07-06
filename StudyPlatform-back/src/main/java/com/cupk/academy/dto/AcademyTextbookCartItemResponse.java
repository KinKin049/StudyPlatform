package com.cupk.academy.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
