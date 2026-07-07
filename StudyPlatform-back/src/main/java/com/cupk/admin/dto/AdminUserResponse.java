package com.cupk.admin.dto;

/**
 * 管理员端用户信息响应DTO，用于返回用户的详细信息（含积分数据）
 */
public record AdminUserResponse(
        /**
         * 用户ID
         */
        long id,

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
         * 学校名称
         */
        String school,

        /**
         * 教师姓名
         */
        String teacherName,

        /**
         * 总积分
         */
        long coinTotal,

        /**
         * 积分调整值
         */
        long coinAdjustment,

        /**
         * 数据备注
         */
        String dataNote,

        /**
         * 是否已完成入职配置
         */
        boolean onboardingCompleted
) {
}
