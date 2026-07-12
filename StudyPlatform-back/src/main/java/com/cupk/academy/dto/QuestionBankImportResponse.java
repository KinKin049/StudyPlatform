package com.cupk.academy.dto;

/**
 * 题库导入响应DTO，用于返回题库题目导入操作的结果信息。
 */
public record QuestionBankImportResponse(
        boolean success,
        int importedProblems,
        int importedTags,
        String message
) {
}
