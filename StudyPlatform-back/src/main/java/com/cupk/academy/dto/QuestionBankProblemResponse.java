package com.cupk.academy.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题库编程题响应DTO，用于返回题库中单个编程题目的详细信息。
 */
public record QuestionBankProblemResponse(
        Long id,
        String source,
        String externalProblemId,
        String title,
        Integer difficulty,
        String difficultyLabel,
        List<String> tagNames,
        String description,
        String inputDescription,
        String outputDescription,
        String hint,
        Integer totalSubmit,
        Integer totalAccepted,
        String sourceUrl,
        LocalDateTime importedAt
) {
}
