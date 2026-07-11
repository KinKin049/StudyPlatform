package com.cupk.academy.dto;

import java.util.List;

public record ProfileUserResponse(
        long userId,
        String name,
        String email,
        String handle,
        String role,
        String roleType,
        String teacherName,
        String bio,
        String location,
        List<String> metaTags,
        String school,
        String avatarUrl
) {
}
