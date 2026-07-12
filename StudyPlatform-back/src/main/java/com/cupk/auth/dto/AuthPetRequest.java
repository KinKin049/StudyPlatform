package com.cupk.auth.dto;

/**
 * 用户宠物更新请求DTO，用于更新用户选择的宠物标识。
 */
public record AuthPetRequest(
        /**
         * 宠物标识
         */
        String petKey
) {
}