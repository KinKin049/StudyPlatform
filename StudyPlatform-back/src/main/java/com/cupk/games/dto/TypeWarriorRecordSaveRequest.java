package com.cupk.games.dto;

/**
 * 打字勇士游戏记录保存请求。
 */
public record TypeWarriorRecordSaveRequest(
        /**
         * 到达的波次
         */
        Integer reachedWave,
        /**
         * 完成的波次数量
         */
        Integer completedWaveCount,
        /**
         * 得分
         */
        Long score,
        /**
         * 最大连击数
         */
        Integer maxCombo,
        /**
         * 解决的单词数量
         */
        Integer solvedWordCount,
        /**
         * 击杀敌人总数
         */
        Integer totalKillCount,
        /**
         * 输入的字母总数
         */
        Integer typedLetterCount,
        /**
         * 游戏总时长（秒）
         */
        Double durationSeconds,
        /**
         * 有效打字时长（秒）
         */
        Double effectiveTypingSeconds
) {
}