package com.cupk.academy.dto;

/**
 * 首页项目响应DTO，用于返回首页各板块的项目信息。
 */
public record AcademyHomeItemResponse(
        String title,
        String category,
        String meta
) {
}
