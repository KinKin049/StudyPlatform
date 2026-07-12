package com.cupk.academy.dto;

import java.util.List;

/**
 * 作业提交响应DTO，用于返回作业提交后的批改结果汇总。
 */
public record AcademyAssignmentSubmitResponse(
        String status,
        Integer score,
        Integer autoScore,
        Integer pendingScore,
        Boolean pendingTeacherReview,
        String message,
        List<AcademyAssignmentQuestionResultResponse> questionResults
) {
}
