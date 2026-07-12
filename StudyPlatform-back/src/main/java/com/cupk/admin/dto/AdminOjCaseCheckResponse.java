package com.cupk.admin.dto;

/**
 * 管理员OJ测试用例校验响应DTO，用于返回单个测试用例的校验结果。
 */
public record AdminOjCaseCheckResponse(
        /**
         * 排序序号
         */
        Integer sortOrder,
        /**
         * 输入数据
         */
        String inputData,
        /**
         * 期望输出
         */
        String expectedOutput,
        /**
         * 实际输出
         */
        String actualOutput,
        /**
         * 是否匹配
         */
        Boolean matched,
        /**
         * 校验消息
         */
        String message
) {
}