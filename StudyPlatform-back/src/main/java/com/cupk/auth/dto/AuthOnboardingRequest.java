package com.cupk.auth.dto;

import java.util.List;

/**
 * 用户入职配置请求DTO，用于保存注册后首次登录的角色和个人资料选择
 */
public record AuthOnboardingRequest(
        /**
         * 用户ID
         */
        Long userId,

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
        String petKey
) {
}
