package com.cupk.academy.dto;

/**
 * 随机组卷请求DTO，用于接收用户生成随机考试的请求参数。
 */
public record AcademyRandomExamRequest(
        Long userId,
        Integer questionCount,
        Integer durationMinutes
) {
}
