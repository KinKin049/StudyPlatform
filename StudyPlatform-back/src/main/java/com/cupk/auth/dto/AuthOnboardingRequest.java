package com.cupk.auth.dto;

import java.util.List;

/**
 * Saves the role and first-run profile choices after registration.
 */
public record AuthOnboardingRequest(
        Long userId,
        String roleType,
        String learningGoal,
        List<String> interests,
        String school,
        String teacherName,
        String petKey
) {
}
