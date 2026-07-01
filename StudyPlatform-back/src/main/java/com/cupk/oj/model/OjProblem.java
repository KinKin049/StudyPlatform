package com.cupk.oj.model;

import java.time.LocalDateTime;

public record OjProblem(
        Long id,
        String title,
        String slug,
        String description,
        String inputDescription,
        String outputDescription,
        String samples,
        ProblemDifficulty difficulty,
        Integer timeLimitMs,
        Integer memoryLimitKb,
        String tags,
        ProblemStatus status,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
