package com.cupk.oj.model;

import java.time.LocalDateTime;

public record OjSubmissionCase(
        Long id,
        Long submissionId,
        Long testCaseId,
        SubmissionStatus status,
        Integer timeUsedMs,
        Integer memoryUsedKb,
        String message,
        LocalDateTime createdAt
) {
}
