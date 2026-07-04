package com.cupk.academy.dto;

import java.util.List;

public record QuestionBankProblemPageResponse(
        List<QuestionBankProblemResponse> items,
        int page,
        int size,
        long total
) {
}
