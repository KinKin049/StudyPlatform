package com.cupk.admin.dto;

public record AdminCourseResponse(
        String resourceType,
        String id,
        String name,
        String teacher,
        String category,
        String school,
        String coverUrl,
        String coverFilePath,
        String startTime,
        Integer participants,
        String comment,
        String description,
        String semesterPlan,
        String overview,
        String videoFilePath,
        String link,
        boolean certified
) {
}
