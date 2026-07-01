package com.cupk.academy.dto;

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
