package com.cupk.games.controller;

import com.cupk.games.dto.LadderJumpQuestionResponse;
import com.cupk.games.service.LadderJumpQuestionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台跳跃小游戏题目接口。
 */
@RestController
@RequestMapping("/api/games/ladder-jump")
public class LadderJumpQuestionController {
    private final LadderJumpQuestionService questionService;

    public LadderJumpQuestionController(LadderJumpQuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/questions")
    public List<LadderJumpQuestionResponse> listQuestions() {
        return questionService.listQuestions();
    }
}
