package com.cupk.academy.dto;

/**
 * 教师工作台指标响应DTO，用于返回教师工作台的各类统计指标。
 */
public record TeacherWorkbenchMetricResponse(
        String label,
        int value,
        String color
) {
}
