package com.cupk.academy.dto;

import java.util.List;

public record CourseQuestionBankQuestionResponse(
        long id,
        String type,
        String stem,
        List<String> options,
        String answer,
        String explanation,
        String difficultyLabel,
        String sourceUrl
) {
}
