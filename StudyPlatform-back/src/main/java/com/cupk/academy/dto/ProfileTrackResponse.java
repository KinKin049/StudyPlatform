package com.cupk.academy.dto;

public record ProfileTrackResponse(
        String name,
        int progress,
        String solved,
        String tone
) {
}
