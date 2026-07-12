package com.cupk.academy.dto;

import java.time.LocalDateTime;

/**
 * 考试摘要响应DTO，用于返回考试列表中单个考试的简要信息。
 */
public record AcademyExamSummaryResponse(
        String id,
        String title,
        String course,
        String teacher,
        String status,
        LocalDateTime startsAt,
        LocalDateTime deadline,
        Integer attemptsLeft,
        Integer durationMinutes,
        Integer totalScore,
        String description,
        Integer questionCount,
        String submissionStatus,
        Integer score,
        Boolean pendingTeacherReview,
        LocalDateTime startedAt,
        LocalDateTime submittedAt
) {
}
