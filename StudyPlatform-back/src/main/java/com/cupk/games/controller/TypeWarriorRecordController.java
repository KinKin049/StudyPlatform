package com.cupk.games.controller;

import com.cupk.games.dto.TypeWarriorRecordSaveRequest;
import com.cupk.games.service.GameRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives Type Warrior run summaries from the frontend.
 */
@RestController
@RequestMapping("/api/games/type-warrior")
public class TypeWarriorRecordController {
    private static final long DEFAULT_USER_ID = 1L;

    private final GameRecordService gameRecordService;

    public TypeWarriorRecordController(GameRecordService gameRecordService) {
        this.gameRecordService = gameRecordService;
    }

    @PostMapping("/records")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveRecord(
            @RequestHeader(value = "X-Auth-User-Id", required = false) Long userId,
            @RequestBody TypeWarriorRecordSaveRequest request
    ) {
        gameRecordService.saveTypeWarriorRecord(resolveUserId(userId), request);
    }

    private long resolveUserId(Long userId) {
        return userId == null || userId <= 0 ? DEFAULT_USER_ID : userId;
    }
}
