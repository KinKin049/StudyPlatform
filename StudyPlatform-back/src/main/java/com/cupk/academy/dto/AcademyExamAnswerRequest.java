package com.cupk.academy.dto;

import java.util.Map;

public record AcademyExamAnswerRequest(
        Long userId,
        Map<String, Object> answers
) {
}
