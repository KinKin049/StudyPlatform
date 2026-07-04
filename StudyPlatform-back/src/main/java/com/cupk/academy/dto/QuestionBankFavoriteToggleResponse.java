package com.cupk.academy.dto;

public record QuestionBankFavoriteToggleResponse(
        long questionId,
        boolean favorited,
        long total,
        String message
) {
}
