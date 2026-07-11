package com.cupk.academy.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TeacherAssignmentCreateRequest(
        String courseId,
        String title,
        String description,
        LocalDateTime deadline,
        Integer attemptsLimit,
        Integer durationMinutes,
        List<TeacherAssignmentQuestionRequest> questions
) {
}
