package com.cupk.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank(message = "请输入邮箱")
        @Email(message = "请输入正确的邮箱")
        String email,

        @NotBlank(message = "请输入新密码")
        @Size(min = 6, max = 72, message = "新密码长度需要在 6 到 72 个字符之间")
        String password,

        @NotBlank(message = "请确认新密码")
        String confirmPassword,

        @NotBlank(message = "请输入验证码")
        String code
) {
}
