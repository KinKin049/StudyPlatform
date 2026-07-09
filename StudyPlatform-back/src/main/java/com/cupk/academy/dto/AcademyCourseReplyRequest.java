package com.cupk.academy.dto;

import jakarta.validation.constraints.NotBlank;

public record AcademyCourseReplyRequest(
        @NotBlank String content
) {
}
