package com.cupk.academy.dto;

import java.util.List;

/**
 * 首页板块响应DTO，用于返回首页各分类板块的信息和项目列表。
 */
public record AcademyHomeSectionResponse(
        String key,
        String title,
        List<AcademyHomeItemResponse> items
) {
}
