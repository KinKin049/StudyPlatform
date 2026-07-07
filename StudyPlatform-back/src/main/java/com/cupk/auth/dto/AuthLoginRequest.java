package com.cupk.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录请求DTO，用于接收用户登录凭证
 */
public record AuthLoginRequest(
        /**
         * 账号（邮箱地址）
         */
        @NotBlank(message = "请输入邮箱")
        String account,

        /**
         * 密码
         */
        @NotBlank(message = "请输入密码")
        String password
) {
}
