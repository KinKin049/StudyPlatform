package com.cupk.academy.dto;

import java.util.Map;

/**
 * 作业答题请求DTO，用于接收用户提交作业答案的请求参数。
 */
public record AcademyAssignmentAnswerRequest(
        Long userId,
        Map<String, Object> answers
) {
}
