package com.cupk.admin.dto;

import java.util.List;

public record AdminQuestionRequest(
        String setCode,
        String type,
        String stem,
        List<String> options,
        String answer,
        String explanation,
        String difficultyLabel,
        String sourceUrl,
        Integer sortOrder
) {
}
