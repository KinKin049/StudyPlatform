package com.cupk.auth.dto;

import java.util.List;

/**
 * 用户认证响应DTO，用于返回用户信息和认证令牌。
 */
public record AuthUserResponse(
        /**
         * 用户ID
         */
        Long id,
        /**
         * 用户名
         */
        String username,
        /**
         * 邮箱地址
         */
        String email,
        /**
         * 角色类型
         */
        String roleType,
        /**
         * 学习目标
         */
        String learningGoal,
        /**
         * 兴趣标签列表
         */
        List<String> interests,
        /**
         * 学校名称
         */
        String school,
        /**
         * 教师姓名
         */
        String teacherName,
        /**
         * 宠物标识
         */
        String petKey,
        /**
         * 是否完成入职引导
         */
        Boolean onboardingCompleted,
        /**
         * 认证令牌
         */
        String token
) {
    public AuthUserResponse(
            Long id,
            String username,
            String email,
            String roleType,
            String learningGoal,
            List<String> interests,
            String school,
            String teacherName,
            String petKey,
            Boolean onboardingCompleted
    ) {
        this(id, username, email, roleType, learningGoal, interests, school, teacherName, petKey, onboardingCompleted, null);
    }
}