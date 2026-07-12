package com.cupk.academy.dto;

/**
 * 题库科目响应DTO，用于返回题库中各科目分类的信息。
 */
public record QuestionBankSubjectResponse(
        String code,
        String name,
        String description,
        int problemCount
) {
}
