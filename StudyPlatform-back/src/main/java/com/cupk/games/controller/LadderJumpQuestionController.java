package com.cupk.games.controller;

import com.cupk.games.dto.LadderJumpQuestionBankResponse;
import com.cupk.games.dto.LadderJumpQuestionResponse;
import com.cupk.games.service.LadderJumpQuestionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 万题天梯跳题源接口。
 */
@RestController
@RequestMapping("/api/games/ladder-jump")
public class LadderJumpQuestionController {
    private static final long DEFAULT_USER_ID = 1L;

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
            @RequestParam(value = "setCode", required = false) String setCode,
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId
    ) {
        return questionService.listQuestions(resolveUserId(userId), setCode);
    }

    private long resolveUserId(Long userId) {
        return userId == null || userId <= 0 ? DEFAULT_USER_ID : userId;
    }
}
