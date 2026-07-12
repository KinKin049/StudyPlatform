package com.cupk.academy.dto;

import java.util.List;

/**
 * 教师布置作业请求DTO，用于接收教师创建作业的请求参数。
 */
public record AcademyTeacherAssignmentRequest(
        String courseId,
        String title,
        String deadlineAt,
        Integer attemptsLimit,
        Integer durationMinutes,
        String description,
        List<AcademyTeacherAssignmentQuestionRequest> questions
) {
}
