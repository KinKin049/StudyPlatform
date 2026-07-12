package com.cupk.academy.dto;

/**
 * 用户档案学习路径响应DTO，用于返回用户在各学习路径上的进度信息。
 */
public record ProfileTrackResponse(
        String name,
        int progress,
        String solved,
        String tone
) {
}
