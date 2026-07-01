package com.cupk.academy.dto;

public record AcademyCourseResponse(
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
        String link
) {
}
