package com.cupk.academy.dto;

import java.time.LocalDateTime;

/**
 * 收藏夹题集摘要响应DTO，用于返回收藏夹中单个题集的统计信息。
 */
public record QuestionBankFavoriteSetSummaryResponse(
        String setCode,
        String setTitle,
        String categoryName,
        long total,
        LocalDateTime latestAt
) {
}
