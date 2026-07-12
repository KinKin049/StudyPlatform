package com.cupk.academy.dto;

/**
 * 错题本答题响应DTO，用于返回错题重练后的答题结果和掌握状态。
 */
public record QuestionBankMistakeAnswerResponse(
        long questionId,
        boolean correct,
        boolean inMistakeBook,
        int wrongCount,
        int correctStreak,
        boolean mastered,
        String message
) {
}
