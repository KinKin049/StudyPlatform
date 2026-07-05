package com.cupk.games.dto;

/**
 * Persists one Type Warrior run.
 */
public record TypeWarriorRecordSaveRequest(
        Integer reachedWave,
        Integer completedWaveCount,
        Long score,
        Integer maxCombo,
        Integer solvedWordCount,
        Integer totalKillCount,
        Integer typedLetterCount,
        Double durationSeconds,
        Double effectiveTypingSeconds
) {
}
