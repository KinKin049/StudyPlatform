package com.cupk.oj.dto;

import com.cupk.oj.model.SubmissionStatus;
import java.util.List;

public record JudgeResult(
        SubmissionStatus status,
        Integer score,
        Integer timeUsedMs,
        Integer memoryUsedKb,
        String message,
        List<JudgeCaseResult> cases
) {
}
