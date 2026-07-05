package com.cupk.academy.dto;

import java.time.LocalDateTime;

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
        String link,
        LocalDateTime enrolledAt
) {
}
