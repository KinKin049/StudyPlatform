package com.cupk.academy.dto;

/**
 * 用户档案最近活动响应DTO，用于返回用户最近的学习活动记录。
 */
public record ProfileRecentActivityResponse(
        String title,
        String meta
) {
}
