package com.cupk.academy.dto;

import java.util.List;

/**
 * 收藏夹分页响应DTO，用于返回收藏夹题目列表的分页数据。
 */
public record QuestionBankFavoritePageResponse(
        List<QuestionBankFavoriteResponse> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
