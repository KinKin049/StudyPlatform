package com.cupk.academy.dto;

import java.time.LocalDateTime;

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
