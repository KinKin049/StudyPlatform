package com.cupk.academy.dto;

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
