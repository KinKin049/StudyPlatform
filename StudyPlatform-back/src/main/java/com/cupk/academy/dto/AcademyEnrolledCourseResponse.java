package com.cupk.academy.dto;

import java.time.LocalDateTime;

/**
 * 已报名课程响应DTO，用于返回用户已报名课程的详细信息。
 */
public record AcademyEnrolledCourseResponse(
        String resourceType,
        String id,
        String name,
        String teacher,
        String category,
        String school,
        String cover,
        String coverUrl,
        String coverFilePath,
        String startTime,
        Integer participants,
        String comment,
        String description,
        String semesterPlan,
        String overview,
        String video,
        String videoFilePath,
        String link,
        LocalDateTime enrolledAt
) {
}
