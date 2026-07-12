package com.cupk.academy.dto;

/**
 * 课程响应DTO，用于返回在线公开课、普通课程或微专业课程的详细信息。
 */
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
        String semesterPlan,
        String overview,
        String video,
        String videoFilePath,
        String link,
        boolean certified,
        String certificationLabel
) {
}
