package com.cupk.academy.dto;

import java.time.LocalDateTime;

public record QuestionBankMistakeSetSummaryResponse(
        String setCode,
        String setTitle,
        String categoryName,
        long total,
        long active,
        long mastered,
        LocalDateTime latestAt
) {
}
