package com.cupk.admin.dto;

public record AdminOjTestCaseResponse(
        Long id,
        Long problemId,
        String inputData,
        String expectedOutput,
        Boolean sample,
        Integer weight,
        Integer sortOrder
) {
}
