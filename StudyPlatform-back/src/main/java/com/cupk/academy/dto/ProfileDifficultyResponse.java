package com.cupk.academy.dto;

public record ProfileDifficultyResponse(
        String label,
        long solved,
        long total,
        String color
) {
}
