package com.cupk.academy.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AcademyCourseReviewResponse(
        Long id,
        Long parentReviewId,
        Long userId,
        String userName,
        String userRoleType,
        int rating,
        String content,
        LocalDateTime createdAt,
        String replyContent,
        String replyUserName,
        String replyUserRoleType,
        LocalDateTime repliedAt,
        List<AcademyCourseReviewResponse> replies
) {
}
