package com.cupk.games.dto;

/**
 * 万题天梯跳可用题库信息。
 */
public record LadderJumpQuestionBankResponse(
        /**
         * 题库编码
         */
        String code,
        /**
         * 题库标题
         */
        String title,
        /**
         * 分类名称
         */
        String categoryName,
        /**
         * 题目数量
         */
        int questionCount
) {
}