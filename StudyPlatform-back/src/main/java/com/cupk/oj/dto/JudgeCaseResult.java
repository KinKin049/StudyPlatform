package com.cupk.oj.dto;

import com.cupk.oj.model.SubmissionStatus;

/**
 * 测试用例判题结果DTO，用于返回单个测试用例的执行结果。
 */
public record JudgeCaseResult(
        Long testCaseId,
        SubmissionStatus status,
        Integer timeUsedMs,
        Integer memoryUsedKb,
        String message,
        String actualOutput
) {
}
