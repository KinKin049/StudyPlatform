package com.cupk.admin.dto;

public record AdminOjTestCaseRequest(
        Long id,
        String inputData,
        String expectedOutput,
        Boolean sample,
        Integer weight,
        Integer sortOrder
) {
}
