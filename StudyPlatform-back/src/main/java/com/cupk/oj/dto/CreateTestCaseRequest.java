package com.cupk.oj.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建测试用例请求DTO。
 */
public record CreateTestCaseRequest(
        /** 输入数据。 */
        @NotBlank String inputData,
        /** 期望输出。 */
        @NotBlank String expectedOutput,
        /** 是否为样例。 */
        @NotNull Boolean sample,
        /** 权重。 */
        @NotNull @Min(1) Integer weight,
        /** 排序序号。 */
        @NotNull @Min(0) Integer sortOrder
) {
}
