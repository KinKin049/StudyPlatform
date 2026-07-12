package com.cupk.academy.dto;

/**
 * 教材响应DTO，用于返回精品教材的基本信息。
 */
public record AcademyTextbookResponse(
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
        String link
) {
}
