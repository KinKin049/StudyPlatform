package com.cupk.academy.dto;

/**
 * 作业题目结果响应DTO，用于返回作业提交后单个题目的批改结果。
 */
public record AcademyAssignmentQuestionResultResponse(
        Long questionId,
        String status,
        Integer score,
        Integer maxScore,
        Boolean pendingTeacherReview,
        String message
) {
}
