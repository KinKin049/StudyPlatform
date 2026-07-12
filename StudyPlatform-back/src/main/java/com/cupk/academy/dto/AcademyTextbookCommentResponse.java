package com.cupk.academy.dto;

/**
 * 教材评论响应DTO，用于返回教材评论的详细信息。
 */
public record AcademyTextbookCommentResponse(
        String user,
        int rating,
        String content,
        String replyContent,
        String replyUserName,
        String replyUserRoleType
) {
}
