package com.cupk.academy.dto;

import java.util.List;

public record QuestionBankMistakeSummaryResponse(
        long total,
        long active,
        long mastered,
        List<QuestionBankMistakeSetSummaryResponse> sets
) {
}
