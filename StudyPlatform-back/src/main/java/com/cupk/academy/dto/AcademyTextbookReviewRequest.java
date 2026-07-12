package com.cupk.academy.dto;

/**
 * 教材评论请求DTO，用于接收用户对教材发表评论的请求参数。
 */
public record AcademyTextbookReviewRequest(
        Long userId,
        String userName,
        Integer rating,
        String content
) {
}
