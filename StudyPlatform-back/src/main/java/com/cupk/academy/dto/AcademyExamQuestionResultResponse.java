package com.cupk.academy.dto;

/**
 * 考试题目结果响应DTO，用于返回考试提交后单个题目的批改结果。
 */
public record AcademyExamQuestionResultResponse(
        Long questionId,
        String status,
        Integer score,
        Integer maxScore,
        Boolean pendingTeacherReview,
        String message
) {
}
