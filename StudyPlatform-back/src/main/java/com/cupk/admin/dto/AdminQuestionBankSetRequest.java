package com.cupk.admin.dto;

import java.util.List;

public record AdminQuestionBankSetRequest(
        String categoryCode,
        String categoryName,
        String categoryDescription,
        String code,
        String title,
        String subtitle,
        String description,
        String coverUrl,
        String coverFilePath,
        String difficultyLabel,
        String statusLabel,
        String sourceName,
        String sourceUrl,
        List<String> sourceRefs,
        String routePath,
        Integer sortOrder
) {
}
