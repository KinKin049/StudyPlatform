package com.cupk.admin.dto;

public record AdminUserResponse(
        long id,
        String username,
        String email,
        String roleType,
        String learningGoal,
        String school,
        String teacherName,
        long coinAdjustment,
        String dataNote,
        boolean onboardingCompleted
) {
}
