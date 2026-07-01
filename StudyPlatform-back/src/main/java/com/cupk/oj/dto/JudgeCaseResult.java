package com.cupk.oj.dto;

import com.cupk.oj.model.SubmissionStatus;

public record JudgeCaseResult(
        Long testCaseId,
        SubmissionStatus status,
        Integer timeUsedMs,
        Integer memoryUsedKb,
        String message
) {
}
