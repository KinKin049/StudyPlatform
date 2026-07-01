package com.cupk.oj.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTestCaseRequest(
        @NotBlank String inputData,
        @NotBlank String expectedOutput,
        @NotNull Boolean sample,
        @NotNull @Min(1) Integer weight,
        @NotNull @Min(0) Integer sortOrder
) {
}
