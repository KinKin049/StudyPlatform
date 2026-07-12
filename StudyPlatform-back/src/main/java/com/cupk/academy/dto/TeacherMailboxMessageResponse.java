package com.cupk.academy.dto;

import java.time.LocalDateTime;

/**
 * 教师邮箱消息响应DTO，用于返回教师邮箱中的消息详情。
 */
public record TeacherMailboxMessageResponse(
        Long id,
        Long parentReviewId,
        String courseId,
        String courseTitle,
        Long userId,
        String userName,
        String userRoleType,
        String content,
        LocalDateTime createdAt,
        boolean unread
) {
}
