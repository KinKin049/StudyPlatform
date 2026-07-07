package com.cupk.oj.dto;

import com.cupk.oj.model.SubmissionStatus;

/**
 * 测试用例判题结果DTO。
 */
public record JudgeCaseResult(
        /** 测试用例ID。 */
        Long testCaseId,
        /** 状态。 */
        SubmissionStatus status,
        /** 用时（毫秒）。 */
        Integer timeUsedMs,
        /** 内存使用（千字节）。 */
        Integer memoryUsedKb,
        /** 消息。 */
        String message
) {
}
