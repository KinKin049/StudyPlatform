package com.cupk.academy.dto;

import java.util.List;

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
