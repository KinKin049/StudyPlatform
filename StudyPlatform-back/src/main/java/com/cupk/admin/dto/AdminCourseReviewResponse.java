package com.cupk.admin.dto;

import java.time.LocalDateTime;

/**
 * 管理员课程评论响应DTO，用于返回评论的详细信息。
 */
public record AdminCourseReviewResponse(
        /**
         * 评论ID
         */
        long id,
        /**
         * 评论类型
         */
        String reviewType,
        /**
         * 资源类型
         */
        String resourceType,
        /**
         * 目标ID
         */
        String targetId,
        /**
         * 父评论ID
         */
        Long parentReviewId,
        /**
         * 父评论用户名
         */
        String parentUserName,
        /**
         * 用户ID
         */
        Long userId,
        /**
         * 用户名
         */
        String userName,
        /**
         * 用户邮箱
         */
        String userEmail,
        /**
         * 用户角色类型
         */
        String userRoleType,
        /**
         * 评分
         */
        int rating,
        /**
         * 评论内容
         */
        String content,
        /**
         * 创建时间
         */
        LocalDateTime createdAt,
        /**
         * 回复内容
         */
        String replyContent,
        /**
         * 回复用户ID
         */
        Long replyUserId,
        /**
         * 回复用户名
         */
        String replyUserName,
        /**
         * 回复用户角色类型
         */
        String replyUserRoleType,
        /**
         * 回复时间
         */
        LocalDateTime repliedAt
) {
}