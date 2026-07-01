package com.cupk.oj.model;

import java.time.LocalDateTime;

public record OjTestCase(
        Long id,
        Long problemId,
        String inputData,
        String expectedOutput,
        Boolean sample,
        Integer weight,
        Integer sortOrder,
        LocalDateTime createdAt
) {
}
