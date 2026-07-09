package com.cupk.academy.dto;

public record AcademyTextbookCommentResponse(
        String user,
        int rating,
        String content,
        String replyContent,
        String replyUserName,
        String replyUserRoleType
) {
}
