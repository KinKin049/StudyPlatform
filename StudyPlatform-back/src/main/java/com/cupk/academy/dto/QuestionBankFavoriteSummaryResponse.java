package com.cupk.academy.dto;

import java.util.List;

/**
 * 收藏夹摘要响应DTO，用于返回用户收藏夹的总体统计和各题集的简要信息。
 */
public record QuestionBankFavoriteSummaryResponse(
        long total,
        List<QuestionBankFavoriteSetSummaryResponse> sets
) {
}
