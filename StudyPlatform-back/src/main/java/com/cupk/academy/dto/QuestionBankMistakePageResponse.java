package com.cupk.academy.dto;

import java.util.List;

/**
 * 错题本分页响应DTO，用于返回错题本错题列表的分页数据。
 */
public record QuestionBankMistakePageResponse(
        List<QuestionBankMistakeResponse> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
