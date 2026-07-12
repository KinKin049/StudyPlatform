package com.cupk.academy.dto;

import java.time.LocalDateTime;

/**
 * 作业摘要响应DTO，用于返回作业列表中单个作业的简要信息。
 */
public record AcademyAssignmentSummaryResponse(
        String id,
        String title,
        String course,
        String teacher,
        String status,
        LocalDateTime deadline,
        Integer attemptsLeft,
        Integer durationMinutes,
        Integer totalScore,
        String description,
        Integer questionCount,
        String submissionStatus,
        Integer score,
        Boolean pendingTeacherReview
) {
}
