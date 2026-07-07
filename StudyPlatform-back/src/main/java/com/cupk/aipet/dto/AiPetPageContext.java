package com.cupk.aipet.dto;

import java.util.List;

/**
 * AI宠物页面上下文信息。
 */
public record AiPetPageContext(
        /**
         * 当前页面路径
         */
        String path,
        /**
         * 当前页面标题
         */
        String title,
        /**
         * 页面标题列表
         */
        List<String> headings,
        /**
         * 页面文本片段
         */
        String textSnippet
) {
}