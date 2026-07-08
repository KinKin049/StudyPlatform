package com.cupk.academy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AcademyCourseReviewRequest(
        @Min(1) @Max(5) int rating,
        @NotBlank String content,
        Long parentReviewId
) {
}
