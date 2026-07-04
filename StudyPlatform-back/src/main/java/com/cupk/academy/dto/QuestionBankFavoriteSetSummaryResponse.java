package com.cupk.academy.dto;

import java.time.LocalDateTime;

public record QuestionBankFavoriteSetSummaryResponse(
        String setCode,
        String setTitle,
        String categoryName,
        long total,
        LocalDateTime latestAt
) {
}
