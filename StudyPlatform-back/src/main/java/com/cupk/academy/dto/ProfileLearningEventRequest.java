package com.cupk.academy.dto;

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
