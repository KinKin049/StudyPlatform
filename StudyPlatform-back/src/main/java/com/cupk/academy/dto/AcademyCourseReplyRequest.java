package com.cupk.academy.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 课程评论回复请求DTO，用于接收对课程评论进行回复的请求参数。
 */
public record AcademyCourseReplyRequest(
        @NotBlank String content
) {
}
