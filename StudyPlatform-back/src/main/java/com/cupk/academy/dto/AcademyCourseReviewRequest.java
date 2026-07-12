package com.cupk.academy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 课程评论请求DTO，用于接收用户对课程发表评论的请求参数。
 */
public record AcademyCourseReviewRequest(
        @Min(1) @Max(5) int rating,
        @NotBlank String content,
        Long parentReviewId
) {
}
