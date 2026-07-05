package com.cupk.academy.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AcademyAssignmentDetailResponse(
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
        List<AcademyAssignmentQuestionResponse> questions,
        Map<String, Object> draftAnswers,
        String submissionStatus,
        Integer score,
        Boolean pendingTeacherReview
) {
}
