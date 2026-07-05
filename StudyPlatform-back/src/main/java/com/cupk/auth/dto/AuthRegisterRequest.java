package com.cupk.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Carries user supplied registration credentials.
 */
public record AuthRegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名不能超过 64 个字符")
        String username,

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱不能超过 128 个字符")
        String email,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 72, message = "密码长度需要在 6 到 72 个字符之间")
        String password,

        @NotBlank(message = "确认密码不能为空")
        String confirmPassword,

        @NotNull(message = "请先同意用户协议")
        @AssertTrue(message = "请先同意用户协议")
        Boolean agreementAccepted
) {
}
