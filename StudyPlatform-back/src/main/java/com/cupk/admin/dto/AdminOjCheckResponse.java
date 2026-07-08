package com.cupk.admin.dto;

import java.util.List;

public record AdminOjCheckResponse(
        Boolean passed,
        String message,
        List<AdminOjCaseCheckResponse> cases
) {
}
