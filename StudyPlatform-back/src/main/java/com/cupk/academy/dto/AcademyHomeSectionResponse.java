package com.cupk.academy.dto;

import java.util.List;

public record AcademyHomeSectionResponse(
        String key,
        String title,
        List<AcademyHomeItemResponse> items
) {
}
