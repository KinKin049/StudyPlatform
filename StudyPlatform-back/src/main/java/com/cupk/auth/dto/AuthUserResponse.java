package com.cupk.auth.dto;

import java.util.List;

/**
 * Returns the currently authenticated lightweight user profile.
 */
public record AuthUserResponse(
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
}
