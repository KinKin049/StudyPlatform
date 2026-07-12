package com.cupk.admin.dto;

/**
 * 管理员评论回复请求DTO，用于接收回复评论的参数。
 */
public record AdminReviewReplyRequest(
        /**
         * 回复内容
         */
        String content
) {
}