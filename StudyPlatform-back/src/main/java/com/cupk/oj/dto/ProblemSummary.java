package com.cupk.oj.dto;

import com.cupk.oj.model.ProblemDifficulty;
import com.cupk.oj.model.ProblemStatus;
import java.time.LocalDateTime;

/**
 * 题目摘要DTO。
 */
public record ProblemSummary(
        /** 题目ID。 */
        Long id,
        /** 题目标题。 */
        String title,
        /** 题目别名。 */
        String slug,
        /** 题目难度。 */
        ProblemDifficulty difficulty,
        /** 时间限制（毫秒）。 */
        Integer timeLimitMs,
        /** 内存限制（千字节）。 */
        Integer memoryLimitKb,
        /** 标签。 */
        String tags,
        /** 题目状态。 */
        ProblemStatus status,
        /** 创建时间。 */
        LocalDateTime createdAt,
        /** 更新时间。 */
        LocalDateTime updatedAt
) {
}
