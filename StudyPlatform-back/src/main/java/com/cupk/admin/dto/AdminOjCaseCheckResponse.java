package com.cupk.admin.dto;

public record AdminOjCaseCheckResponse(
        Integer sortOrder,
        String inputData,
        String expectedOutput,
        String actualOutput,
        Boolean matched,
        String message
) {
}
