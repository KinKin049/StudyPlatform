package com.cupk.academy.dto;

public record ProfileActivityDayResponse(
        int id,
        String date,
        int count,
        int level
) {
}
