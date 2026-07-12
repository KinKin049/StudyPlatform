package com.cupk.academy.dto;

/**
 * 用户档案统计响应DTO，用于返回用户学习档案中的单个统计数据。
 */
public record ProfileStatResponse(
        String label,
        String value,
        String hint
) {
}
