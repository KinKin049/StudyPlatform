package com.cupk.academy.dto;

public record ProfileUserResponse(
        long userId,
        String name,
        String handle,
        String role,
        String roleType,
        String teacherName,
        String bio,
        String location,
        String school,
        String avatarUrl
) {
}
