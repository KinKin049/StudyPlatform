package com.cupk.aipet.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record AiPetChatRequest(
        @NotBlank(message = "请输入想问星云学习猫的问题")
        String message,
        AiPetPageContext pageContext,
        List<AiPetChatMessage> history
) {
}
