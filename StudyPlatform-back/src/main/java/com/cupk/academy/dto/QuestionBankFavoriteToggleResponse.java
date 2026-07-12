package com.cupk.academy.dto;

/**
 * 收藏夹收藏切换响应DTO，用于返回题目收藏/取消收藏后的状态。
 */
public record QuestionBankFavoriteToggleResponse(
        long questionId,
        boolean favorited,
        long total,
        String message
) {
}
