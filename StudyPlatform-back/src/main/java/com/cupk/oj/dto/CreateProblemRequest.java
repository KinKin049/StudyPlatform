package com.cupk.oj.dto;

import com.cupk.oj.model.ProblemDifficulty;
import com.cupk.oj.model.ProblemStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建题目请求DTO
 */
public record CreateProblemRequest(
        /**
         * 题目标题
         */
        @NotBlank @Size(max = 128) String title,
        /**
         * 题目短标识
         */
        @NotBlank @Size(max = 128) String slug,
        /**
         * 题目描述
         */
        @NotBlank String description,
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
        @NotNull ProblemDifficulty difficulty,
        /**
         * 时间限制（毫秒），范围100-30000
         */
        @NotNull @Min(100) @Max(30000) Integer timeLimitMs,
        /**
         * 内存限制（KB），范围1024-1048576
         */
        @NotNull @Min(1024) @Max(1048576) Integer memoryLimitKb,
        /**
         * 标签（逗号分隔）
         */
        String tags,
        /**
         * 题目状态
         */
        @NotNull ProblemStatus status,
        /**
         * 创建者ID
         */
        Long createdBy
) {
}