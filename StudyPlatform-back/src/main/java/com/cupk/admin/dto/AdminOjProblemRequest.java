package com.cupk.admin.dto;

import java.util.List;

/**
 * 管理员OJ题目请求DTO，用于接收新增或更新OJ题目的参数。
 */
public record AdminOjProblemRequest(
        /**
         * 题目ID
         */
        Long id,
        /**
         * 题目标题
         */
        String title,
        /**
         * 题目标识
         */
        String slug,
        /**
         * 题目分类
         */
        String category,
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
         * 标准代码
         */
        String standardCode,
        /**
         * 题目难度
         */
        String difficulty,
        /**
         * 时间限制（毫秒）
         */
        Integer timeLimitMs,
        /**
         * 内存限制（KB）
         */
        Integer memoryLimitKb,
        /**
         * 标签
         */
        String tags,
        /**
         * 题目状态
         */
        String status,
        /**
         * 测试用例列表
         */
        List<AdminOjTestCaseRequest> testCases
) {
}