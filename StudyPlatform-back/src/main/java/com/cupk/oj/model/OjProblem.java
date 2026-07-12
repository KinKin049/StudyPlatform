package com.cupk.oj.model;

import java.time.LocalDateTime;

/**
 * OJ题目实体，包含题目的基本信息、描述、限制条件和状态。
 */
public record OjProblem(
        Long id,
        String title,
        String slug,
        String category,
        String description,
        String inputDescription,
        String outputDescription,
        String standardCode,
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
