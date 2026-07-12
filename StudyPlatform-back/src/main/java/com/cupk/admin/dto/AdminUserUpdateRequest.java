package com.cupk.admin.dto;

/** 管理员用户更新请求DTO，用于接收管理后台更新用户信息的请求参数。 */
public record AdminUserUpdateRequest(
        /** 用户名 */
        String username,
        /** 邮箱 */
        String email,
        /** 密码 */
        String password,
        /** 角色类型 */
        String roleType,
        /** 学习目标 */
        String learningGoal,
        /** 学校 */
        String school,
        /** 教师姓名 */
        String teacherName,
        /** 金币总数 */
        Long coinTotal,
        /** 金币调整量 */
        Long coinAdjustment,
        /** 数据备注 */
        String dataNote
) {
}
