package com.cupk.oj.dto;

import com.cupk.oj.model.ProblemDifficulty;
import com.cupk.oj.model.ProblemStatus;
import java.time.LocalDateTime;

public record ProblemSummary(
        Long id,
        String title,
        String slug,
        ProblemDifficulty difficulty,
        Integer timeLimitMs,
        Integer memoryLimitKb,
        String tags,
        ProblemStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
