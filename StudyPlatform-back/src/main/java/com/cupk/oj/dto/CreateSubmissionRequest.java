package com.cupk.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSubmissionRequest(
        @NotNull Long problemId,
        Long userId,
        @NotBlank @Size(max = 32) String language,
        @NotBlank @Size(max = 200000) String sourceCode
) {
}
