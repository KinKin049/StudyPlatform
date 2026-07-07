package com.cupk.games.controller;

import com.cupk.games.dto.LadderJumpRecordSaveRequest;
import com.cupk.games.service.GameRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 天梯跳游戏记录控制器
 * 提供天梯跳游戏记录保存相关接口
 */
@RestController
@RequestMapping("/api/games/ladder-jump")
public class LadderJumpRecordController {
    private static final long DEFAULT_USER_ID = 1L;

    private final GameRecordService gameRecordService;

    public LadderJumpRecordController(GameRecordService gameRecordService) {
        this.gameRecordService = gameRecordService;
    }

    /**
     * 保存天梯跳游戏记录
     * @param userId 用户ID，从请求头获取，可选
     * @param request 游戏记录保存请求
     */
    @PostMapping("/records")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveRecord(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @RequestBody LadderJumpRecordSaveRequest request
    ) {
        gameRecordService.saveLadderJumpRecord(resolveUserId(userId), request);
    }

    private long resolveUserId(Long userId) {
        return userId == null || userId <= 0 ? DEFAULT_USER_ID : userId;
    }
}
