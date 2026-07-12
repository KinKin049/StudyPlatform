package com.cupk.academy.dto;

/**
 * 用户档案学习事件请求DTO，用于接收用户学习事件记录的请求参数。
 */
public record ProfileLearningEventRequest(
        String eventType,
        String setCode,
        Long questionId,
        String questionType,
        String selectedAnswer,
        String correctAnswer,
        Boolean isCorrect,
        String vocabularyStatus
) {
}
