package com.cupk.academy.dto;

import java.util.List;

public record CourseQuestionBankQuestionPageResponse(
        List<CourseQuestionBankQuestionResponse> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
