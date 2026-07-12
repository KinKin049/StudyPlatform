package com.cupk.academy.dto;

import java.time.LocalDateTime;

/**
 * 错题本题集摘要响应DTO，用于返回错题本中单个题集的统计信息。
 */
public record QuestionBankMistakeSetSummaryResponse(
        String setCode,
        String setTitle,
        String categoryName,
        long total,
        long active,
        long mastered,
        LocalDateTime latestAt
) {
}
