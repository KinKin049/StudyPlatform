package com.cupk.academy.dto;

/**
 * 用户档案学习时长记录请求DTO，用于接收用户记录学习时长的请求参数。
 */
public record ProfileLearningTimeRecordRequest(
        String moduleType,
        String targetCode,
        String targetTitle,
        Integer durationSeconds
) {
}
