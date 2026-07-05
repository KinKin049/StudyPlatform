package com.cupk.academy.dto;

public record AcademyExamQuestionResultResponse(
        Long questionId,
        String status,
        Integer score,
        Integer maxScore,
        Boolean pendingTeacherReview,
        String message
) {
}
