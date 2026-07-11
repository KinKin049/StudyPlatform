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
         * 当前 AI 宠物完整名称
         */
        String petName,
        /**
         * 当前 AI 宠物展示短名称
         */
        String petShortName,
        /**
         * 当前 AI 宠物形象标识
         */
        String petKey,
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
