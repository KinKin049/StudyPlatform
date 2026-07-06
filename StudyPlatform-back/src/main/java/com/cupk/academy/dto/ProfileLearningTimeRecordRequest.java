package com.cupk.academy.dto;

public record ProfileLearningTimeRecordRequest(
        String moduleType,
        String targetCode,
        String targetTitle,
        Integer durationSeconds
) {
}
