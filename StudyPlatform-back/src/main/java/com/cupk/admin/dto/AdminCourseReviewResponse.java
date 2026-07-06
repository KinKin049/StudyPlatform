package com.cupk.admin.dto;

import java.time.LocalDateTime;

public record AdminCourseReviewResponse(
        long id,
        String resourceType,
        String courseId,
        String userName,
        int rating,
        String content,
        LocalDateTime createdAt
) {
}
