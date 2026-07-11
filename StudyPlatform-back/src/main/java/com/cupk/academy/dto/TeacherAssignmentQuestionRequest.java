package com.cupk.academy.dto;

import java.util.List;

public record TeacherAssignmentQuestionRequest(
        String type,
        String label,
        String title,
        List<String> options,
        String placeholder,
        Integer score,
        Object correctAnswer,
        String explanation
) {
}
