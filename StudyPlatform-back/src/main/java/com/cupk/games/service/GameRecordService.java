package com.cupk.games.service;

import com.cupk.games.dto.LadderJumpRecordSaveRequest;
import com.cupk.games.dto.TypeWarriorRecordSaveRequest;
import com.cupk.games.repository.GameRecordRepository;
import com.cupk.rewards.CoinRewardService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 游戏记录服务，负责验证和存储游戏会话记录，并触发金币奖励。
 */
@Service
public class GameRecordService {
    private final GameRecordRepository gameRecordRepository;
    private final CoinRewardService coinRewardService;

    /**
     * 构造函数，注入依赖的仓库和服务。
     *
     * @param gameRecordRepository 游戏记录数据访问层
     * @param coinRewardService    金币奖励服务
     */
    public GameRecordService(GameRecordRepository gameRecordRepository, CoinRewardService coinRewardService) {
        this.gameRecordRepository = gameRecordRepository;
        this.coinRewardService = coinRewardService;
    }

    /**
     * 保存万题天梯跳游戏记录。
     *
     * @param userId  用户ID
     * @param request 游戏记录请求对象
     */
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

    /**
     * 保存 Type Warrior 游戏记录。
     *
     * @param userId  用户ID
     * @param request 游戏记录请求对象
     */
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

    /**
     * 验证整数值非负。
     *
     * @param value    待验证值
     * @param fieldName 字段名称
     */
    private void validateNonNegative(Integer value, String fieldName) {
        if (value != null && value < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + "不能小于 0");
        }
    }

    /**
     * 验证长整数值非负。
     *
     * @param value    待验证值
     * @param fieldName 字段名称
     */
    private void validateNonNegative(Long value, String fieldName) {
        if (value != null && value < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + "不能小于 0");
        }
    }

    /**
     * 验证浮点数值非负。
     *
     * @param value    待验证值
     * @param fieldName 字段名称
     */
    private void validateNonNegative(Double value, String fieldName) {
        if (value != null && value < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + "不能小于 0");
        }
    }
}
