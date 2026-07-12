package com.cupk.academy.dto;

/**
 * 收藏夹收藏请求DTO，用于接收用户收藏或取消收藏题目的请求参数。
 */
public record QuestionBankFavoriteRequest(
        long questionId
) {
}
