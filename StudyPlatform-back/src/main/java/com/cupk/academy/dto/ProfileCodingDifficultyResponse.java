package com.cupk.academy.dto;

public record ProfileCodingDifficultyResponse(
        String label,
        String level,
        long solved,
        long total,
        String color
) {
}
