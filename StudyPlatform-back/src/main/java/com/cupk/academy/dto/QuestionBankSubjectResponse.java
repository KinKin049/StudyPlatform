package com.cupk.academy.dto;

public record QuestionBankSubjectResponse(
        String code,
        String name,
        String description,
        int problemCount
) {
}
