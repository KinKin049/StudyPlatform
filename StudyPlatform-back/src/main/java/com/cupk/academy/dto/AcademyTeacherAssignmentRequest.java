package com.cupk.academy.dto;

import java.util.List;

public record AcademyTeacherAssignmentRequest(
        String courseId,
        String title,
        String deadlineAt,
        Integer attemptsLimit,
        Integer durationMinutes,
        String description,
        List<AcademyTeacherAssignmentQuestionRequest> questions
) {
}
