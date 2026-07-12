package com.cupk.academy.dto;

/**
 * 用户档案学习时长响应DTO，用于返回用户在不同模块的学习时长统计。
 */
public record ProfileLearningTimeResponse(
        String label,
        String value,
        String hint,
        String tone
) {
}
