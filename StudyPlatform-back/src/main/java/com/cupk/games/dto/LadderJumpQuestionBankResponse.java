package com.cupk.games.dto;

/**
 * 万题天梯跳可用题库信息。
 */
public record LadderJumpQuestionBankResponse(
        String code,
        String title,
        String categoryName,
        int questionCount
) {
}
