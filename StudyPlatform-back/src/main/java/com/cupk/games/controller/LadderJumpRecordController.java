package com.cupk.games.controller;

import com.cupk.games.dto.LadderJumpRecordSaveRequest;
import com.cupk.games.service.GameRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives ladder jump run summaries from the frontend.
 */
@RestController
@RequestMapping("/api/games/ladder-jump")
public class LadderJumpRecordController {
    private final GameRecordService gameRecordService;

    public LadderJumpRecordController(GameRecordService gameRecordService) {
        this.gameRecordService = gameRecordService;
    }

    @PostMapping("/records")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveRecord(@RequestBody LadderJumpRecordSaveRequest request) {
        gameRecordService.saveLadderJumpRecord(request);
    }
}
