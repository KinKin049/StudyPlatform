package com.cupk.admin.dto;

import java.util.List;

public record AdminOjProblemResponse(
        Long id,
        String title,
        String slug,
        String category,
        String description,
        String inputDescription,
        String outputDescription,
        String standardCode,
        String difficulty,
        Integer timeLimitMs,
        Integer memoryLimitKb,
        String tags,
        String status,
        List<AdminOjTestCaseResponse> testCases
) {
}
