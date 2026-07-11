package com.cupk.academy.dto;

public record AcademyRandomExamRequest(
        Long userId,
        Integer questionCount,
        Integer durationMinutes
) {
}
