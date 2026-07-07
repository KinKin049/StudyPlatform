package com.cupk.aipet.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * AI宠物聊天请求。
 */
public record AiPetChatRequest(
        /**
         * 用户输入的消息内容
         */
        @NotBlank(message = "请输入想问星云学习猫的问题")
        String message,
        /**
         * 当前页面上下文信息
         */
        AiPetPageContext pageContext,
        /**
         * 聊天历史记录列表
         */
        List<AiPetChatMessage> history
) {
}