package com.cupk.academy.dto;

/**
 * 课程报名请求DTO，用于接收用户报名课程的请求参数。
 */
public record AcademyCourseEnrollmentRequest(
        Long userId
) {
}
