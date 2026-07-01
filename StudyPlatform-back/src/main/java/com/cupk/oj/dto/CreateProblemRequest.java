package com.cupk.oj.dto;

import com.cupk.oj.model.ProblemDifficulty;
import com.cupk.oj.model.ProblemStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProblemRequest(
        @NotBlank @Size(max = 128) String title,
        @NotBlank @Size(max = 128) String slug,
        @NotBlank String description,
        String inputDescription,
        String outputDescription,
        String samples,
        @NotNull ProblemDifficulty difficulty,
        @NotNull @Min(100) @Max(30000) Integer timeLimitMs,
        @NotNull @Min(1024) @Max(1048576) Integer memoryLimitKb,
        String tags,
        @NotNull ProblemStatus status,
        Long createdBy
) {
}
