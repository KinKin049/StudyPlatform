package com.cupk.admin.dto;

/**
 * 管理员OJ测试用例响应DTO，用于返回OJ测试用例的详细信息。
 */
public record AdminOjTestCaseResponse(
        /**
         * 测试用例ID
         */
        Long id,
        /**
         * 题目ID
         */
        Long problemId,
        /**
         * 输入数据
         */
        String inputData,
        /**
         * 期望输出
         */
        String expectedOutput,
        /**
         * 是否示例
         */
        Boolean sample,
        /**
         * 权重
         */
        Integer weight,
        /**
         * 排序序号
         */
        Integer sortOrder
) {
}