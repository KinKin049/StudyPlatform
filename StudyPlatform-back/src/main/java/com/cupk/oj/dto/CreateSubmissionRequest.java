package com.cupk.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建提交请求DTO。
 */
public record CreateSubmissionRequest(
        /** 题目ID。 */
        @NotNull Long problemId,
        /** 用户ID。 */
        Long userId,
        /** 编程语言。 */
        @NotBlank @Size(max = 32) String language,
        /** 源代码。 */
        @NotBlank @Size(max = 200000) String sourceCode
) {
}
