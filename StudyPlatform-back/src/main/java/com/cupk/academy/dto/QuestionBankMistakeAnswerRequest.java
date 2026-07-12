package com.cupk.academy.dto;

/**
 * 错题本答题请求DTO，用于接收用户对错题进行重练答题的请求参数。
 */
public record QuestionBankMistakeAnswerRequest(
        long questionId,
        String selectedAnswer
) {
}
