package com.cupk.academy.dto;

import java.time.LocalDateTime;
import java.util.List;

public record QuestionBankFavoriteResponse(
        long id,
        long questionId,
        String setCode,
        String setTitle,
        String categoryCode,
        String categoryName,
        String type,
        String stem,
        List<String> options,
        String answer,
        String explanation,
        String difficultyLabel,
        String sourceUrl,
        LocalDateTime createdAt
) {
}
