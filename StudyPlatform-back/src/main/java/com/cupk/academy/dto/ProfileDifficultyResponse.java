package com.cupk.academy.dto;

/**
 * 用户档案难度分布响应DTO，用于返回用户在不同难度题目上的完成情况。
 */
public record ProfileDifficultyResponse(
        String label,
        long solved,
        long total,
        String color
) {
}
