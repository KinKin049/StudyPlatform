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
         * 当前路由名称
         */
        String routeName,
        /**
         * 当前页面标题
         */
        String title,
        /**
         * 页面标题列表
         */
        List<String> headings,
        /**
         * 用户当前选中的页面文本
         */
        String selectedText,
        /**
         * 页面表单或输入控件的安全摘要
         */
        List<String> formSnapshot,
        /**
         * 页面可见文本长度
         */
        Integer contentLength,
        /**
         * 页面文本片段
         */
        String textSnippet
) {
}
