package com.cupk.academy.dto;

public record QuestionBankImportResponse(
        boolean success,
        int importedProblems,
        int importedTags,
        String message
) {
}
