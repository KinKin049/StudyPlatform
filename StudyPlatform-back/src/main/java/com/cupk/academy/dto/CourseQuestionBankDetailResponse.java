package com.cupk.academy.dto;

import java.util.List;

public record CourseQuestionBankDetailResponse(
        CourseQuestionBankSetResponse bank,
        List<CourseQuestionBankQuestionResponse> questions,
        int page,
        int size,
        long total,
        int totalPages
) {
}
