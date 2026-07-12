package com.cupk.academy.dto;

import java.util.List;

/**
 * 作业题目响应DTO，用于返回作业中单个题目的详细信息。
 */
public record AcademyAssignmentQuestionResponse(
        Long id,
        String type,
        String label,
        String title,
        List<String> options,
        String placeholder,
        Integer score,
        Boolean autoGradable,
        Long ojProblemId,
        Boolean requiresTeacherReview,
        String explanation
) {
}
