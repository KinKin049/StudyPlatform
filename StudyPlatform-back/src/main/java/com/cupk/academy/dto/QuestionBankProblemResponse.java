package com.cupk.academy.dto;

import java.time.LocalDateTime;
import java.util.List;

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
