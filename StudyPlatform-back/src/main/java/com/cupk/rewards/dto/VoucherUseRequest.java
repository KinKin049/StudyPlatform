package com.cupk.rewards.dto;

/**
 * 卡券使用请求DTO，用于接收用户使用卡券的请求参数。
 */
public record VoucherUseRequest(String voucherKey) {
}
