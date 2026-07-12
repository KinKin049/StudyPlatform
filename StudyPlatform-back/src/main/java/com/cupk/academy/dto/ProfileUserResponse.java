package com.cupk.academy.dto;

import java.util.List;

/**
 * 用户档案响应DTO，用于返回用户的完整个人资料信息。
 */
public record ProfileUserResponse(
        long userId,
        String name,
        String email,
        String handle,
        String role,
        String roleType,
        String teacherName,
        String bio,
        String location,
        List<String> metaTags,
        String school,
        String avatarUrl
) {
}
