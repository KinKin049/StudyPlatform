package com.cupk.academy.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 教材详情响应DTO，用于返回精品教材的完整信息，包括价格、目录和评论。
 */
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
