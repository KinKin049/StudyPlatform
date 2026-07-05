package com.cupk.academy.dto;

import java.util.List;

public record AcademyExamSubmitResponse(
        String status,
        Integer score,
        Integer autoScore,
        Integer pendingScore,
        Boolean pendingTeacherReview,
        String message,
        List<AcademyExamQuestionResultResponse> questionResults
) {
}
