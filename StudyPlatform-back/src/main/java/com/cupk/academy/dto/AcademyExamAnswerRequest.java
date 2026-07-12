package com.cupk.academy.dto;

import java.util.Map;

/**
 * 考试答题请求DTO，用于接收用户提交考试答案的请求参数。
 */
public record AcademyExamAnswerRequest(
        Long userId,
        Map<String, Object> answers
) {
}
