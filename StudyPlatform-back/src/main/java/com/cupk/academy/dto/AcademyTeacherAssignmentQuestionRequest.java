package com.cupk.academy.dto;

import java.util.List;

public record AcademyTeacherAssignmentQuestionRequest(
        String type,
        String label,
        String title,
        List<String> options,
        String placeholderText,
        Integer score,
        Object correctAnswer,
        String explanation,
        Boolean autoGradable,
        Long ojProblemId,
        Boolean requiresTeacherReview
) {
}
