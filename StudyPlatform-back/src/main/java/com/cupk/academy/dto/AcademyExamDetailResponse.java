package com.cupk.academy.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 考试详情响应DTO，用于返回考试的完整信息，包括题目列表和用户答题记录。
 */
public record AcademyExamDetailResponse(
        String id,
        String title,
        String course,
        String teacher,
        String status,
        LocalDateTime startsAt,
        LocalDateTime deadline,
        Integer attemptsLeft,
        Integer durationMinutes,
        Integer totalScore,
        String description,
        List<AcademyExamQuestionResponse> questions,
        Map<String, Object> draftAnswers,
        String submissionStatus,
        Integer score,
        Boolean pendingTeacherReview,
        LocalDateTime startedAt,
        LocalDateTime submittedAt
) {
}
