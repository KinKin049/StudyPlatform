package com.cupk.academy.dto;

import java.util.List;

public record CourseQuestionBankCategoryResponse(
        String code,
        String name,
        String description,
        List<CourseQuestionBankSetResponse> sets
) {
}
