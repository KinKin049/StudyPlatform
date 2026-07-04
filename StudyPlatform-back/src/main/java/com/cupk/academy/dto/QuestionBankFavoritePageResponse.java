package com.cupk.academy.dto;

import java.util.List;

public record QuestionBankFavoritePageResponse(
        List<QuestionBankFavoriteResponse> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
