package com.cupk.academy.dto;

import java.util.List;

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
