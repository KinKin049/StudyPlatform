package com.cupk.academy.dto;

import java.util.List;

/**
 * 用户档案概览响应DTO，用于返回用户学习档案的综合概览信息。
 */
public record ProfileOverviewResponse(
        List<ProfileStatResponse> stats,
        int overallProgress,
        List<ProfileDifficultyResponse> difficultyStats,
        List<ProfileTrackResponse> skillTracks,
        List<ProfileRecentActivityResponse> recentActivities,
        List<String> badges,
        List<ProfileActivityDayResponse> activityDays,
        List<ProfileLearningTimeResponse> learningTimes,
        List<ProfileCodingDifficultyResponse> codingDifficulties,
        List<ProfilePreviewMetricResponse> gameMetrics,
        long coinTotal,
        List<ProfilePreviewMetricResponse> mistakeMetrics,
        List<ProfilePreviewMetricResponse> rankingMetrics,
        List<ProfilePreviewMetricResponse> achievementMetrics,
        List<ProfilePreviewMetricResponse> textbookOrders
) {
}
