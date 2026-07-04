package com.cupk.academy.dto;

public record ProfileUserResponse(
        long userId,
        String name,
        String handle,
        String role,
        String bio,
        String location,
        String school,
        String avatarUrl
) {
}
