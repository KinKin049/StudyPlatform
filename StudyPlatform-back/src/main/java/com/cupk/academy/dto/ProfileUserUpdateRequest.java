package com.cupk.academy.dto;

import java.util.List;

/**
 * 用户档案更新请求DTO，用于接收用户更新个人资料的请求参数。
 */
public record ProfileUserUpdateRequest(
        String name,
        String email,
        String bio,
        String location,
        List<String> metaTags,
        String currentPassword,
        String newPassword,
        String confirmNewPassword
) {
}
