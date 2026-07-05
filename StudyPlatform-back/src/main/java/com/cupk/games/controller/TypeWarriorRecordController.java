package com.cupk.games.controller;

import com.cupk.games.dto.TypeWarriorRecordSaveRequest;
import com.cupk.games.service.GameRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives Type Warrior run summaries from the frontend.
 */
@RestController
@RequestMapping("/api/games/type-warrior")
public class TypeWarriorRecordController {
    private final GameRecordService gameRecordService;

    public TypeWarriorRecordController(GameRecordService gameRecordService) {
        this.gameRecordService = gameRecordService;
    }

    @PostMapping("/records")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveRecord(@RequestBody TypeWarriorRecordSaveRequest request) {
        gameRecordService.saveTypeWarriorRecord(request);
    }
}
