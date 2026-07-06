package com.cupk.academy.dto;

public record AcademyTextbookReviewRequest(
        Long userId,
        String userName,
        Integer rating,
        String content
) {
}
