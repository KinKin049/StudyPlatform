package com.cupk.academy.dto;

import java.time.LocalDateTime;

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
