package com.cupk.oj.dto;

import com.cupk.oj.model.ProblemDifficulty;
import com.cupk.oj.model.ProblemStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 更新题目请求DTO。
 */
public record UpdateProblemRequest(
        /** 题目标题。 */
        @Size(max = 80) String category,
        @NotBlank @Size(max = 128) String title,
        /** 题目描述。 */
        @NotBlank String description,
        /** 输入描述。 */
        String inputDescription,
        /** 输出描述。 */
        String outputDescription,
        /** 样例数据。 */
        String samples,
        /** 题目难度。 */
        @NotNull ProblemDifficulty difficulty,
        /** 时间限制（毫秒）。 */
        @NotNull @Min(100) @Max(30000) Integer timeLimitMs,
        /** 内存限制（千字节）。 */
        @NotNull @Min(1024) @Max(1048576) Integer memoryLimitKb,
        /** 标签。 */
        String tags,
        /** 题目状态。 */
        @NotNull ProblemStatus status
) {
}
