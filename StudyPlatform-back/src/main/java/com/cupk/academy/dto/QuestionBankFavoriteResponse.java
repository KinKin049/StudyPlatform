package com.cupk.academy.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 收藏夹题目响应DTO，用于返回收藏夹中单个题目的详细信息。
 */
public record QuestionBankFavoriteResponse(
        long id,
        long questionId,
        String setCode,
        String setTitle,
        String categoryCode,
        String categoryName,
        String type,
        String stem,
        List<String> options,
        String answer,
        String explanation,
        String difficultyLabel,
        String sourceUrl,
        LocalDateTime createdAt
) {
}
