package com.cupk.academy.dto;

import java.math.BigDecimal;
import java.util.List;

public record AcademyTextbookDetailResponse(
        String id,
        String name,
        String editor,
        String category,
        String publisher,
        String publishDate,
        String isbn,
        String description,
        String cover,
        String coverUrl,
        String coverFilePath,
        String link,
        String recommendation,
        BigDecimal originalPrice,
        BigDecimal discountPrice,
        Integer readerCount,
        String overview,
        List<String> catalog,
        List<AcademyTextbookCommentResponse> comments,
        boolean purchased
) {
}
