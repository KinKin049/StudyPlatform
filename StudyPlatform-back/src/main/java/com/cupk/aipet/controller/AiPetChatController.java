package com.cupk.aipet.controller;

import com.cupk.aipet.dto.AiPetChatRequest;
import com.cupk.aipet.dto.AiPetChatResponse;
import com.cupk.aipet.service.AiPetChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI宠物聊天控制器
 * 提供AI宠物对话交互相关接口
 */
@RestController
@RequestMapping("/api/ai-pet")
public class AiPetChatController {
    private final AiPetChatService aiPetChatService;

    public AiPetChatController(AiPetChatService aiPetChatService) {
        this.aiPetChatService = aiPetChatService;
    }

    /**
     * 与AI宠物进行对话
     * @param request 聊天请求，包含对话内容
     * @return 聊天响应，包含AI宠物回复
     */
    @PostMapping("/chat")
    public AiPetChatResponse chat(@Valid @RequestBody AiPetChatRequest request) {
        return new AiPetChatResponse(aiPetChatService.chat(request));
    }
}
