package com.cupk.academy.dto;

import java.util.Map;

public record AcademyAssignmentAnswerRequest(
        Long userId,
        Map<String, Object> answers
) {
}
