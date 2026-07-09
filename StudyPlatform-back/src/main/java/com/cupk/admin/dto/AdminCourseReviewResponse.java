package com.cupk.admin.dto;

import java.time.LocalDateTime;

public record AdminCourseReviewResponse(
        long id,
        String reviewType,
        String resourceType,
        String targetId,
        Long parentReviewId,
        String parentUserName,
        Long userId,
        String userName,
        String userEmail,
        String userRoleType,
        int rating,
        String content,
        LocalDateTime createdAt,
        String replyContent,
        Long replyUserId,
        String replyUserName,
        String replyUserRoleType,
        LocalDateTime repliedAt
) {
}
