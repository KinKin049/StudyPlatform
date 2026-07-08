package com.cupk.admin.dto;

public record AdminCourseRequest(
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
        Boolean certified,
        String certificationLabel
) {
}
