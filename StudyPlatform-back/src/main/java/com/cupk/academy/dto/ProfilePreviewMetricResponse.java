package com.cupk.academy.dto;

/**
 * 用户档案预览指标响应DTO，用于返回用户档案中的各类预览指标数据。
 */
public record ProfilePreviewMetricResponse(
        String title,
        String value,
        String meta,
        String tone
) {
}
