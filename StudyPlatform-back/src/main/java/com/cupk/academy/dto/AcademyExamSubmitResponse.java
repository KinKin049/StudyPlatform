package com.cupk.academy.dto;

import java.util.List;

/**
 * 考试提交响应DTO，用于返回考试提交后的批改结果汇总。
 */
public record AcademyExamSubmitResponse(
        String status,
        Integer score,
        Integer autoScore,
        Integer pendingScore,
        Boolean pendingTeacherReview,
        String message,
        List<AcademyExamQuestionResultResponse> questionResults
) {
}
