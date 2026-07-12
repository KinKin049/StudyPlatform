package com.cupk.rewards.dto;

/**
 * 卡券兑换请求DTO，用于接收用户兑换卡券的请求参数。
 */
public record VoucherExchangeRequest(String voucherKey) {
}
