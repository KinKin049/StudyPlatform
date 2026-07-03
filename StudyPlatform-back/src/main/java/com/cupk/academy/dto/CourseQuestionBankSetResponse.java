package com.cupk.academy.dto;

import java.util.List;

public record CourseQuestionBankSetResponse(
        long id,
        String code,
        String title,
        String subtitle,
        String description,
        String categoryCode,
        String categoryName,
        String coverUrl,
        String fallbackCoverUrl,
        String coverFilePath,
        int questionCount,
        String difficultyLabel,
        String statusLabel,
        String sourceName,
        String sourceUrl,
        List<String> sourceRefs,
        String routePath
) {
}
