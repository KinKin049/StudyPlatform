package com.cupk.admin.dto;

import java.util.List;

/**
 * 管理员题库集合请求DTO，用于接收新增或更新题库集合的参数。
 */
public record AdminQuestionBankSetRequest(
        /**
         * 分类编号
         */
        String categoryCode,
        /**
         * 分类名称
         */
        String categoryName,
        /**
         * 分类描述
         */
        String categoryDescription,
        /**
         * 题库编号
         */
        String code,
        /**
         * 题库名称
         */
        String title,
        /**
         * 题库副标题
         */
        String subtitle,
        /**
         * 题库描述
         */
        String description,
        /**
         * 封面URL
         */
        String coverUrl,
        /**
         * 封面本地路径
         */
        String coverFilePath,
        /**
         * 难度标签
         */
        String difficultyLabel,
        /**
         * 状态标签
         */
        String statusLabel,
        /**
         * 来源名称
         */
        String sourceName,
        /**
         * 来源URL
         */
        String sourceUrl,
        /**
         * 来源参考列表
         */
        List<String> sourceRefs,
        /**
         * 路由路径
         */
        String routePath,
        /**
         * 排序序号
         */
        Integer sortOrder
) {
}