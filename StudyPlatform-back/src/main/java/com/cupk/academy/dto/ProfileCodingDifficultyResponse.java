package com.cupk.academy.dto;

/**
 * 用户档案编程难度响应DTO，用于返回用户在不同编程难度题目上的完成情况。
 */
public record ProfileCodingDifficultyResponse(
        String label,
        String level,
        long solved,
        long total,
        String color
) {
}
