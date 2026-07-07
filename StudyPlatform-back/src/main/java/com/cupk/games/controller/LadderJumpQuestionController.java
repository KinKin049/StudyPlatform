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
 * 天梯跳题目控制器
 * 提供天梯跳游戏题库列表和题目列表相关接口
 */
@RestController
@RequestMapping("/api/games/ladder-jump")
public class LadderJumpQuestionController {
    private static final long DEFAULT_USER_ID = 1L;

    private final LadderJumpQuestionService questionService;

    public LadderJumpQuestionController(LadderJumpQuestionService questionService) {
        this.questionService = questionService;
    }

    /**
     * 获取天梯跳题库列表
     * @return 题库列表响应
     */
    @GetMapping("/question-banks")
    public List<LadderJumpQuestionBankResponse> listQuestionBanks() {
        return questionService.listQuestionBanks();
    }

    /**
     * 获取天梯跳题目列表
     * @param setCode 题库编码，可选
     * @param userId 用户ID，从请求头获取，可选
     * @return 题目列表响应
     */
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
