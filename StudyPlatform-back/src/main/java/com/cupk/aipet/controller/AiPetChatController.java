package com.cupk.aipet.controller;

import com.cupk.aipet.dto.AiPetChatRequest;
import com.cupk.aipet.dto.AiPetChatResponse;
import com.cupk.aipet.service.AiPetChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-pet")
public class AiPetChatController {
    private final AiPetChatService aiPetChatService;

    public AiPetChatController(AiPetChatService aiPetChatService) {
        this.aiPetChatService = aiPetChatService;
    }

    @PostMapping("/chat")
    public AiPetChatResponse chat(@Valid @RequestBody AiPetChatRequest request) {
        return new AiPetChatResponse(aiPetChatService.chat(request));
    }
}
