package com.cupk.oj.model;

import java.time.LocalDateTime;

/**
 * OJ题目实体
 */
public record OjProblem(
        /**
         * 题目ID
         */
        Long id,
        /**
         * 题目标题
         */
        String title,
        /**
         * 题目短标识
         */
        String slug,
        /**
         * 题目描述
         */
        String description,
        /**
         * 输入描述
         */
        String inputDescription,
        /**
         * 输出描述
         */
        String outputDescription,
        /**
         * 示例数据（JSON格式）
         */
        String samples,
        /**
         * 题目难度
         */
        ProblemDifficulty difficulty,
        /**
         * 时间限制（毫秒）
         */
        Integer timeLimitMs,
        /**
         * 内存限制（KB）
         */
        Integer memoryLimitKb,
        /**
         * 标签（逗号分隔）
         */
        String tags,
        /**
         * 题目状态
         */
        ProblemStatus status,
        /**
         * 创建者ID
         */
        Long createdBy,
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