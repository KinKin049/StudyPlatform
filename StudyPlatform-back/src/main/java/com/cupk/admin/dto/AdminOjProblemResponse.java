package com.cupk.admin.dto;

import java.util.List;

/**
 * 管理员OJ题目响应DTO，用于返回OJ题目的详细信息。
 */
public record AdminOjProblemResponse(
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
         * 创建者ID
         */
        Long createdBy,
        /**
         * 所有者名称
         */
        String ownerName,
        /**
         * 所有者角色类型
         */
        String ownerRoleType,
        /**
         * 测试用例列表
         */
        List<AdminOjTestCaseResponse> testCases
) {
}