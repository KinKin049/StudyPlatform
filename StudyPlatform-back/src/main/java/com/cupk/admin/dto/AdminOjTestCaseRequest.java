package com.cupk.admin.dto;

/**
 * 管理员OJ测试用例请求DTO，用于接收新增或更新OJ测试用例的参数。
 */
public record AdminOjTestCaseRequest(
        /**
         * 测试用例ID
         */
        Long id,
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