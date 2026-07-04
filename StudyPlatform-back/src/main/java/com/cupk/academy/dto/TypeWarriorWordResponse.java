package com.cupk.academy.dto;

public record TypeWarriorWordResponse(
        long questionId,
        String setCode,
        String word,
        String text,
        String familiarity,
        int tier
) {
}
