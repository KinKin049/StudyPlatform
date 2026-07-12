package com.cupk.academy.dto;

/**
 * 用户档案活动天数响应DTO，用于返回用户在各日期的学习活动统计。
 */
public record ProfileActivityDayResponse(
        int id,
        String date,
        int count,
        int level
) {
}
