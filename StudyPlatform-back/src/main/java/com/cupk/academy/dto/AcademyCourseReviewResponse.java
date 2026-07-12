package com.cupk.academy.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程评论响应DTO，用于返回课程评论的详细信息，支持嵌套回复结构。
 */
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
