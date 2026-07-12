package com.cupk.academy.dto;

import java.util.List;

/**
 * 题库编程题分页响应DTO，用于返回编程题目列表的分页数据。
 */
public record QuestionBankProblemPageResponse(
        List<QuestionBankProblemResponse> items,
        int page,
        int size,
        long total
) {
}
