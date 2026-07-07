package com.cupk.oj.model;

import java.time.LocalDateTime;

/**
 * OJ提交测试用例结果实体
 */
public record OjSubmissionCase(
        /**
         * 记录ID
         */
        Long id,
        /**
         * 提交ID
         */
        Long submissionId,
        /**
         * 测试用例ID
         */
        Long testCaseId,
        /**
         * 执行状态
         */
        SubmissionStatus status,
        /**
         * 用时（毫秒）
         */
        Integer timeUsedMs,
        /**
         * 内存使用（KB）
         */
        Integer memoryUsedKb,
        /**
         * 执行消息
         */
        String message,
        /**
         * 创建时间
         */
        LocalDateTime createdAt
) {
}