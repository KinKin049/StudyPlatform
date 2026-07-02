package com.cupk.academy.dto;

import java.time.LocalDateTime;

public record AcademyCourseReviewResponse(
        Long id,
        String userName,
        int rating,
        String content,
        LocalDateTime createdAt
) {
}
