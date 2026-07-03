package com.cupk.games.dto;

import java.util.List;

/**
 * 平台跳跃小游戏的三选一题目数据。
 */
public record LadderJumpQuestionResponse(
        Long id,
        String question,
        List<String> options,
        int answerIndex,
        String explanation
) {
}
