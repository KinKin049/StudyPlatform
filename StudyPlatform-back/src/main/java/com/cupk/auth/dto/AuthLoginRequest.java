package com.cupk.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Carries login credentials. The account field is treated as an email address.
 */
public record AuthLoginRequest(
        @NotBlank(message = "请输入邮箱")
        String account,

        @NotBlank(message = "请输入密码")
        String password
) {
}
