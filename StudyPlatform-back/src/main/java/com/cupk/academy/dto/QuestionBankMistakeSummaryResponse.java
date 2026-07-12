package com.cupk.academy.dto;

import java.util.List;

/**
 * 错题本摘要响应DTO，用于返回用户错题本的总体统计和各题集的简要信息。
 */
public record QuestionBankMistakeSummaryResponse(
        long total,
        long active,
        long mastered,
        List<QuestionBankMistakeSetSummaryResponse> sets
) {
}
