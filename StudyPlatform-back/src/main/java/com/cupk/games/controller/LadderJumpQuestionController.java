package com.cupk.games.controller;

import com.cupk.games.dto.LadderJumpQuestionBankResponse;
import com.cupk.games.dto.LadderJumpQuestionResponse;
import com.cupk.games.service.LadderJumpQuestionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 万题天梯跳题源接口。
 */
@RestController
@RequestMapping("/api/games/ladder-jump")
public class LadderJumpQuestionController {
    private final LadderJumpQuestionService questionService;

    public LadderJumpQuestionController(LadderJumpQuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/question-banks")
    public List<LadderJumpQuestionBankResponse> listQuestionBanks() {
        return questionService.listQuestionBanks();
    }

    @GetMapping("/questions")
    public List<LadderJumpQuestionResponse> listQuestions(
            @RequestParam(value = "setCode", required = false) String setCode
    ) {
        return questionService.listQuestions(setCode);
    }
}
