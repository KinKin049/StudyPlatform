package com.cupk.academy.dto;

import java.util.List;

/**
 * 教师布置作业题目请求DTO，用于接收教师为作业添加题目的请求参数。
 */
public record AcademyTeacherAssignmentQuestionRequest(
        String type,
        String label,
        String title,
        List<String> options,
        String placeholderText,
        Integer score,
        Object correctAnswer,
        String explanation,
        Boolean autoGradable,
        Long ojProblemId,
        Boolean requiresTeacherReview
) {
}
