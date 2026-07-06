package com.cupk.games.service;

import com.cupk.games.dto.LadderJumpRecordSaveRequest;
import com.cupk.games.dto.TypeWarriorRecordSaveRequest;
import com.cupk.games.repository.GameRecordRepository;
import com.cupk.rewards.CoinRewardService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates and stores game session records.
 */
@Service
public class GameRecordService {
    private final GameRecordRepository gameRecordRepository;
    private final CoinRewardService coinRewardService;

    public GameRecordService(GameRecordRepository gameRecordRepository, CoinRewardService coinRewardService) {
        this.gameRecordRepository = gameRecordRepository;
        this.coinRewardService = coinRewardService;
    }

    public void saveLadderJumpRecord(long userId, LadderJumpRecordSaveRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少平台跳跃记录参数");
        }
        validateNonNegative(request.totalCoins(), "金币");
        validateNonNegative(request.correctCount(), "答对题数");
        validateNonNegative(request.wrongCount(), "答错题数");
        validateNonNegative(request.durationSeconds(), "游戏时长");
        long recordId = gameRecordRepository.insertLadderJumpRecord(userId, request);
        coinRewardService.rewardLadderJump(userId, recordId, request.totalCoins());
    }

    public void saveTypeWarriorRecord(long userId, TypeWarriorRecordSaveRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少 Type Warrior 记录参数");
        }
        validateNonNegative(request.reachedWave(), "到达波次");
        validateNonNegative(request.completedWaveCount(), "已完成波次");
        validateNonNegative(request.score(), "得分");
        validateNonNegative(request.maxCombo(), "最大连击");
        validateNonNegative(request.solvedWordCount(), "拼对单词数");
        validateNonNegative(request.totalKillCount(), "击杀数");
        validateNonNegative(request.typedLetterCount(), "键入字母数");
        validateNonNegative(request.durationSeconds(), "游戏时长");
        validateNonNegative(request.effectiveTypingSeconds(), "有效输入时长");
        long recordId = gameRecordRepository.insertTypeWarriorRecord(userId, request);
        coinRewardService.rewardTypeWarrior(userId, recordId, request.score());
    }

    private void validateNonNegative(Integer value, String fieldName) {
        if (value != null && value < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + "不能小于 0");
        }
    }

    private void validateNonNegative(Long value, String fieldName) {
        if (value != null && value < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + "不能小于 0");
        }
    }

    private void validateNonNegative(Double value, String fieldName) {
        if (value != null && value < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + "不能小于 0");
        }
    }
}
