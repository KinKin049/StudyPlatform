package com.cupk.oj.model;

import java.time.LocalDateTime;

public record OjSubmission(
        Long id,
        Long problemId,
        Long userId,
        String language,
        String sourceCode,
        SubmissionStatus status,
        Integer score,
        Integer timeUsedMs,
        Integer memoryUsedKb,
        String message,
        LocalDateTime judgedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
