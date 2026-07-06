package com.cupk.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetCodeRequest(
        @NotBlank(message = "请输入邮箱")
        @Email(message = "请输入正确的邮箱")
        String email
) {
}
