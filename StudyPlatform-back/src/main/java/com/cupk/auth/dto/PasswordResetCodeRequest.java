package com.cupk.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 密码重置验证码请求DTO，用于发送验证码到指定邮箱
 */
public record PasswordResetCodeRequest(
        /**
         * 邮箱地址
         */
        @NotBlank(message = "请输入邮箱")
        @Email(message = "请输入正确的邮箱")
        String email
) {
}
