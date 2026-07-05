package com.cupk.academy.dto;

public record AcademyAssignmentQuestionResultResponse(
        Long questionId,
        String status,
        Integer score,
        Integer maxScore,
        Boolean pendingTeacherReview,
        String message
) {
}
