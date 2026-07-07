package com.cupk.oj.model;

import java.time.LocalDateTime;

/**
 * OJ提交记录实体
 */
public record OjSubmission(
        /**
         * 提交ID
         */
        Long id,
        /**
         * 题目ID
         */
        Long problemId,
        /**
         * 用户ID
         */
        Long userId,
        /**
         * 编程语言
         */
        String language,
        /**
         * 源代码
         */
        String sourceCode,
        /**
         * 提交状态
         */
        SubmissionStatus status,
        /**
         * 得分
         */
        Integer score,
        /**
         * 用时（毫秒）
         */
        Integer timeUsedMs,
        /**
         * 内存使用（KB）
         */
        Integer memoryUsedKb,
        /**
         * 评测消息
         */
        String message,
        /**
         * 评测完成时间
         */
        LocalDateTime judgedAt,
        /**
         * 创建时间
         */
        LocalDateTime createdAt,
        /**
         * 更新时间
         */
        LocalDateTime updatedAt
) {
}