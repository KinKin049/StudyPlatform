package com.cupk.aipet.dto;

/**
 * AI宠物聊天消息。
 */
public record AiPetChatMessage(
        /**
         * 消息角色（如 user、assistant）
         */
        String role,
        /**
         * 消息文本内容
         */
        String text
) {
}