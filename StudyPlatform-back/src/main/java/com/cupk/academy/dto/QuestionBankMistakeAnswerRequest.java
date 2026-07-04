package com.cupk.academy.dto;

public record QuestionBankMistakeAnswerRequest(
        long questionId,
        String selectedAnswer
) {
}
