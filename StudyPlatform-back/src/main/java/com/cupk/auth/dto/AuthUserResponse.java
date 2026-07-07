package com.cupk.auth.dto;

import java.util.List;

/**
 * 当前认证用户的轻量级用户资料响应DTO
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
         * 是否已完成入职配置
         */
        Boolean onboardingCompleted
) {
}
