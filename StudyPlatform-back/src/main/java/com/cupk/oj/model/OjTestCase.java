package com.cupk.oj.model;

import java.time.LocalDateTime;

/**
 * OJ测试用例实体
 */
public record OjTestCase(
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
         * 是否为示例用例
         */
        Boolean sample,
        /**
         * 分值权重
         */
        Integer weight,
        /**
         * 排序序号
         */
        Integer sortOrder,
        /**
         * 创建时间
         */
        LocalDateTime createdAt
) {
}