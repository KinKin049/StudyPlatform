package com.cupk.games.dto;

/**
 * Persists one ladder jump run.
 */
public record LadderJumpRecordSaveRequest(
        String questionBankCode,
        Integer totalCoins,
        Integer correctCount,
        Integer wrongCount,
        Double durationSeconds
) {
}
