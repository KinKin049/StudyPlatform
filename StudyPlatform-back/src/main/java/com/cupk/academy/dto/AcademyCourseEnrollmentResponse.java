package com.cupk.academy.dto;

/**
 * 课程报名响应DTO，用于返回报名结果信息。
 */
public record AcademyCourseEnrollmentResponse(
        boolean enrolled,
        String message
) {
}
