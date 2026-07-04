package com.cupk.academy.dto;

import java.util.List;

public record QuestionBankMistakePageResponse(
        List<QuestionBankMistakeResponse> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
