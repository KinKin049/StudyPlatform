package com.cupk.games.dto;

/**
 * 天梯跳游戏记录保存请求。
 */
public record LadderJumpRecordSaveRequest(
        /**
         * 题库编码
         */
        String questionBankCode,
        /**
         * 获得的总金币数
         */
        Integer totalCoins,
        /**
         * 答对题目数量
         */
        Integer correctCount,
        /**
         * 答错题目数量
         */
        Integer wrongCount,
        /**
         * 游戏时长（秒）
         */
        Double durationSeconds
) {
}