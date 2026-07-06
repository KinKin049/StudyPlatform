package com.cupk.academy.dto;

public record AcademyTextbookCartRequest(
        Long userId,
        String textbookId,
        Integer quantity
) {
}
