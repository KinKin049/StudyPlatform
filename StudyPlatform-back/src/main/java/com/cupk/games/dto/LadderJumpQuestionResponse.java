package com.cupk.games.dto;

import java.util.List;

/**
 * 平台跳跃小游戏的三选一题目数据。
 */
public record LadderJumpQuestionResponse(
        /**
         * 题目ID
         */
        Long id,
        /**
         * 题目内容
         */
        String question,
        /**
         * 选项列表
         */
        List<String> options,
        /**
         * 正确答案索引（从0开始）
         */
        int answerIndex,
        /**
         * 答案解析
         */
        String explanation
) {
}