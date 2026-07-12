package com.cupk.admin.dto;

import java.util.List;

/**
 * 管理员题目请求DTO，用于接收新增或更新题目的参数。
 */
public record AdminQuestionRequest(
        /**
         * 题库编号
         */
        String setCode,
        /**
         * 题目类型
         */
        String type,
        /**
         * 题干
         */
        String stem,
        /**
         * 选项列表
         */
        List<String> options,
        /**
         * 答案
         */
        String answer,
        /**
         * 解析
         */
        String explanation,
        /**
         * 难度标签
         */
        String difficultyLabel,
        /**
         * 来源URL
         */
        String sourceUrl,
        /**
         * 排序序号
         */
        Integer sortOrder
) {
}