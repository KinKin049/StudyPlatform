package com.cupk.aipet.dto;

import java.util.List;

public record AiPetPageContext(
        String path,
        String title,
        List<String> headings,
        String textSnippet
) {
}
