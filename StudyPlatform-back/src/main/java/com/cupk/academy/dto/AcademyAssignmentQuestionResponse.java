package com.cupk.academy.dto;

import java.util.List;

public record AcademyAssignmentQuestionResponse(
        Long id,
        String type,
        String label,
        String title,
        List<String> options,
        String placeholder,
        Integer score,
        Boolean autoGradable,
        Long ojProblemId,
        Boolean requiresTeacherReview,
        String explanation
) {
}
