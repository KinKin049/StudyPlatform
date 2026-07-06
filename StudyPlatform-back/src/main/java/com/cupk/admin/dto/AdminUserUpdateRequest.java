package com.cupk.admin.dto;

public record AdminUserUpdateRequest(
        String username,
        String email,
        String password,
        String roleType,
        String learningGoal,
        String school,
        String teacherName,
        Long coinAdjustment,
        String dataNote
) {
}
