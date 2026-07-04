package com.cupk.academy.dto;

import java.util.List;

public record QuestionBankFavoriteSummaryResponse(
        long total,
        List<QuestionBankFavoriteSetSummaryResponse> sets
) {
}
