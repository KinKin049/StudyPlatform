package com.cupk.academy.service;

import com.cupk.academy.dto.ProfileActivityDayResponse;
import com.cupk.academy.dto.ProfileCodingDifficultyResponse;
import com.cupk.academy.dto.ProfileDifficultyResponse;
import com.cupk.academy.dto.ProfileLearningEventRequest;
import com.cupk.academy.dto.ProfileLearningTimeResponse;
import com.cupk.academy.dto.ProfileOverviewResponse;
import com.cupk.academy.dto.ProfilePreviewMetricResponse;
import com.cupk.academy.dto.ProfileRecentActivityResponse;
import com.cupk.academy.dto.ProfileStatResponse;
import com.cupk.academy.dto.ProfileTrackResponse;
import com.cupk.academy.dto.ProfileUserResponse;
import com.cupk.academy.dto.ProfileUserUpdateRequest;
import com.cupk.academy.repository.ProfileRepository;
import com.cupk.games.repository.GameRecordRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileService.class);
    private static final long DEFAULT_USER_ID = 1L;
    private static final int HEATMAP_DAYS = 119;
    private static final long MAX_AVATAR_SIZE = 2L * 1024L * 1024L;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final ProfileRepository profileRepository;
    private final GameRecordRepository gameRecordRepository;

    public ProfileService(ProfileRepository profileRepository, GameRecordRepository gameRecordRepository) {
        this.profileRepository = profileRepository;
        this.gameRecordRepository = gameRecordRepository;
    }

    public ProfileOverviewResponse getOverview() {
        long totalEvents = profileRepository.countEvents(DEFAULT_USER_ID);
        long correctAnswers = profileRepository.countCorrectAnswers(DEFAULT_USER_ID);
        long knownVocabulary = profileRepository.countKnownVocabulary(DEFAULT_USER_ID);
        long vocabularyEvents = profileRepository.countVocabularyEvents(DEFAULT_USER_ID);
        long practicedQuestions = profileRepository.countDistinctPracticedQuestions(DEFAULT_USER_ID);
        long totalQuestions = profileRepository.countTotalQuestions();
        int overallProgress = percentage(practicedQuestions, totalQuestions);
        int streak = currentStreak();

        List<ProfileRepository.TrackRow> trackRows = profileRepository.findTrackRows(DEFAULT_USER_ID);
        GameRecordRepository.LadderJumpAggregateRow ladderJumpAggregate =
                gameRecordRepository.findLadderJumpAggregate(DEFAULT_USER_ID);
        GameRecordRepository.TypeWarriorAggregateRow typeWarriorAggregate =
                gameRecordRepository.findTypeWarriorAggregate(DEFAULT_USER_ID);

        return new ProfileOverviewResponse(
                List.of(
                        new ProfileStatResponse("累计练习", formatNumber(totalEvents), "题 / 词 / 卡片"),
                        new ProfileStatResponse("已掌握", formatNumber(correctAnswers + knownVocabulary), "正确或标记认识"),
                        new ProfileStatResponse("连续学习", String.valueOf(streak), "天"),
                        new ProfileStatResponse("题库进度", overallProgress + "%", "整体完成度")
                ),
                overallProgress,
                buildDistribution(),
                buildTracks(trackRows),
                buildRecentActivities(),
                buildBadges(streak, totalEvents, correctAnswers, knownVocabulary, vocabularyEvents, trackRows),
                buildHeatmap(),
                buildLearningTimes(),
                buildCodingDifficulties(),
                buildGameMetrics(ladderJumpAggregate, typeWarriorAggregate),
                ladderJumpAggregate.totalCoins() + typeWarriorAggregate.totalCoins(),
                buildMistakeMetrics(),
                buildRankingMetrics(),
                buildAchievementMetrics(),
                buildTextbookOrders()
        );
    }

    public void recordLearningEvent(ProfileLearningEventRequest request) {
        profileRepository.insertLearningEvent(DEFAULT_USER_ID, normalizeRequest(request));
    }

    public ProfileUserResponse getUserProfile() {
        return profileRepository.findUserProfile(DEFAULT_USER_ID);
    }

    public ProfileUserResponse updateUserProfile(ProfileUserUpdateRequest request) {
        ProfileUserResponse currentProfile = getUserProfile();
        String name = clean(request == null ? null : request.name(), currentProfile.name(), 64);
        String bio = request == null || request.bio() == null
                ? currentProfile.bio()
                : clean(request.bio(), "", 512);
        if (name == null || name.isBlank()) {
            LOGGER.warn("Profile update rejected: blank display name, userId={}", DEFAULT_USER_ID);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "昵称不能为空");
        }
        profileRepository.updateUserProfile(DEFAULT_USER_ID, name, bio);
        LOGGER.info("Profile user updated successfully: userId={}, displayName={}, bioLength={}",
                DEFAULT_USER_ID, name, bio == null ? 0 : bio.length());
        return getUserProfile();
    }

    public ProfileUserResponse updateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            LOGGER.warn("Profile avatar update rejected: empty file, userId={}", DEFAULT_USER_ID);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择头像图片");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            LOGGER.warn("Profile avatar update rejected: file too large, userId={}, fileSize={}",
                    DEFAULT_USER_ID, file.getSize());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像图片不能超过 2MB");
        }
        String extension = resolveAvatarExtension(file);
        String fileName = "avatar-" + DEFAULT_USER_ID + "-" + UUID.randomUUID() + "." + extension;
        Path avatarDirectory = resolveStoragePath().resolve("profile").resolve("avatars").normalize();
        Path targetPath = avatarDirectory.resolve(fileName).normalize();
        if (!targetPath.startsWith(avatarDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像文件名不合法");
        }
        try {
            Files.createDirectories(avatarDirectory);
            file.transferTo(targetPath);
        } catch (IOException ex) {
            LOGGER.error("Profile avatar save failed: userId={}, targetPath={}", DEFAULT_USER_ID, targetPath, ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "头像保存失败", ex);
        }
        profileRepository.updateAvatarPath(DEFAULT_USER_ID, "profile/avatars/" + fileName);
        LOGGER.info("Profile avatar updated successfully: userId={}, fileName={}, fileSize={}",
                DEFAULT_USER_ID, fileName, file.getSize());
        return getUserProfile();
    }

    private List<ProfileDifficultyResponse> buildDistribution() {
        return profileRepository.findDistributionRows(DEFAULT_USER_ID).stream()
                .filter(row -> row.total() > 0 || row.solved() > 0)
                .map(row -> new ProfileDifficultyResponse(row.label(), row.solved(), row.total(), row.color()))
                .toList();
    }

    private List<ProfileTrackResponse> buildTracks(List<ProfileRepository.TrackRow> trackRows) {
        return trackRows.stream()
                .map(row -> new ProfileTrackResponse(
                        row.name(),
                        percentage(row.practiced(), row.total()),
                        formatNumber(row.practiced()) + " / " + formatNumber(row.total()),
                        toneForCategory(row.code())
                ))
                .toList();
    }

    private List<ProfileRecentActivityResponse> buildRecentActivities() {
        List<ProfileRecentActivityResponse> activities = profileRepository.findRecentEvents(DEFAULT_USER_ID, 6).stream()
                .map(this::toRecentActivity)
                .toList();
        if (!activities.isEmpty()) {
            return activities;
        }
        return List.of(new ProfileRecentActivityResponse(
                "暂无真实练习记录",
                "完成一道题或标记一个单词后，这里会自动刷新"
        ));
    }

    private List<ProfileActivityDayResponse> buildHeatmap() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(HEATMAP_DAYS - 1L);
        Map<LocalDate, Integer> counts = profileRepository.countEventsByDate(DEFAULT_USER_ID, startDate, today);
        List<ProfileActivityDayResponse> days = new ArrayList<>();
        for (int index = 0; index < HEATMAP_DAYS; index += 1) {
            LocalDate date = startDate.plusDays(index);
            int count = counts.getOrDefault(date, 0);
            days.add(new ProfileActivityDayResponse(index, date.toString(), count, activityLevel(count)));
        }
        return days;
    }

    private List<ProfileLearningTimeResponse> buildLearningTimes() {
        long videoSeconds = profileRepository.sumLearningTimeSeconds(DEFAULT_USER_ID, "video");
        long visualizationSeconds = profileRepository.sumLearningTimeSeconds(DEFAULT_USER_ID, "visualization");
        return List.of(
                new ProfileLearningTimeResponse(
                        "视频学习时长",
                        formatDuration(videoSeconds),
                        videoSeconds > 0 ? "来自数据库记录" : "数据库暂无记录",
                        "cyan"
                ),
                new ProfileLearningTimeResponse(
                        "可视化学习时长",
                        formatDuration(visualizationSeconds),
                        visualizationSeconds > 0 ? "来自数据库记录" : "数据库暂无记录",
                        "violet"
                )
        );
    }

    private List<ProfileCodingDifficultyResponse> buildCodingDifficulties() {
        Map<String, ProfileRepository.CodingDifficultyRow> rows = profileRepository.findCodingDifficultyRows(DEFAULT_USER_ID)
                .stream()
                .collect(java.util.stream.Collectors.toMap(ProfileRepository.CodingDifficultyRow::difficulty, row -> row));
        return List.of(
                codingDifficulty("简单", "EASY", "#00b8a3", rows.get("EASY")),
                codingDifficulty("中等", "MEDIUM", "#ffc01e", rows.get("MEDIUM")),
                codingDifficulty("困难", "HARD", "#ef476f", rows.get("HARD"))
        );
    }

    private ProfileCodingDifficultyResponse codingDifficulty(
            String label,
            String level,
            String color,
            ProfileRepository.CodingDifficultyRow row
    ) {
        return new ProfileCodingDifficultyResponse(
                label,
                level,
                row == null ? 0 : row.solved(),
                row == null ? 0 : row.total(),
                color
        );
    }

    private List<ProfilePreviewMetricResponse> buildGameMetrics(
            GameRecordRepository.LadderJumpAggregateRow ladderJump,
            GameRecordRepository.TypeWarriorAggregateRow typeWarrior
    ) {
        GameRecordRepository.CombinedDurationAggregateRow combinedDuration =
                gameRecordRepository.findCombinedDurationAggregate(DEFAULT_USER_ID);

        return List.of(
                new ProfilePreviewMetricResponse(
                        "游戏总时长",
                        formatDurationSeconds(combinedDuration.totalDurationSeconds()),
                        tripleMetricMeta(
                                formatDurationSeconds(combinedDuration.totalDurationSeconds()),
                                formatDurationSeconds(combinedDuration.bestDurationSeconds()),
                                formatDurationSeconds(combinedDuration.averageDurationSeconds())
                        ),
                        "cyan"
                ),
                new ProfilePreviewMetricResponse(
                        "万题天梯跳金币",
                        formatNumber(ladderJump.totalCoins()),
                        tripleMetricMeta(
                                formatNumber(ladderJump.totalCoins()),
                                formatNumber(ladderJump.bestCoins()),
                                formatDecimal(ladderJump.averageCoins())
                        ),
                        "blue"
                ),
                new ProfilePreviewMetricResponse(
                        "万题天梯跳答对题数",
                        formatNumber(ladderJump.totalCorrect()),
                        tripleMetricMeta(
                                formatNumber(ladderJump.totalCorrect()),
                                formatNumber(ladderJump.bestCorrect()),
                                formatDecimal(ladderJump.averageCorrect())
                        ),
                        "cyan"
                ),
                new ProfilePreviewMetricResponse(
                        "Type Warrior 得分",
                        formatNumber(typeWarrior.totalScore()),
                        tripleMetricMeta(
                                formatNumber(typeWarrior.totalScore()),
                                formatNumber(typeWarrior.bestScore()),
                                formatDecimal(typeWarrior.averageScore())
                        ),
                        "violet"
                ),
                new ProfilePreviewMetricResponse(
                        "Type Warrior 金币",
                        formatNumber(typeWarrior.totalCoins()),
                        tripleMetricMeta(
                                formatNumber(typeWarrior.totalCoins()),
                                formatNumber(typeWarrior.bestCoins()),
                                formatDecimal(typeWarrior.averageCoins())
                        ),
                        "blue"
                ),
                new ProfilePreviewMetricResponse(
                        "Type Warrior 击杀数",
                        formatNumber(typeWarrior.totalKills()),
                        tripleMetricMeta(
                                formatNumber(typeWarrior.totalKills()),
                                formatNumber(typeWarrior.bestKills()),
                                formatDecimal(typeWarrior.averageKills())
                        ),
                        "amber"
                ),
                new ProfilePreviewMetricResponse(
                        "Type Warrior 到达波次",
                        formatNumber(typeWarrior.totalReachedWave()),
                        tripleMetricMeta(
                                formatNumber(typeWarrior.totalReachedWave()),
                                formatNumber(typeWarrior.bestReachedWave()),
                                formatDecimal(typeWarrior.averageReachedWave())
                        ),
                        "rose"
                )
        );
    }

    private List<ProfilePreviewMetricResponse> buildMistakeMetrics() {
        return List.of(
                new ProfilePreviewMetricResponse("错题本", "42 题", "待复习 12 题 · 样式预览", "rose"),
                new ProfilePreviewMetricResponse("薄弱知识点", "7 个", "选择题 / 词汇 / 主观题", "amber")
        );
    }

    private List<ProfilePreviewMetricResponse> buildRankingMetrics() {
        return List.of(
                new ProfilePreviewMetricResponse("全站排名", "#128", "超过 82% 学习者", "cyan"),
                new ProfilePreviewMetricResponse("本周排名", "#19", "连续学习加成中", "blue")
        );
    }

    private List<ProfilePreviewMetricResponse> buildAchievementMetrics() {
        return List.of(
                new ProfilePreviewMetricResponse("成就点数", "1,260", "样式预览 · 12 枚徽章", "violet"),
                new ProfilePreviewMetricResponse("稀有成就", "3 枚", "CET / OJ / 可视化", "amber")
        );
    }

    private List<ProfilePreviewMetricResponse> buildTextbookOrders() {
        return List.of(
                new ProfilePreviewMetricResponse("教材订单", "3 单", "待支付 1 · 已完成 2", "cyan"),
                new ProfilePreviewMetricResponse("教材收藏", "18 本", "计算机 / 公共课 / 英语", "blue")
        );
    }

    private List<String> buildBadges(
            int streak,
            long totalEvents,
            long correctAnswers,
            long knownVocabulary,
            long vocabularyEvents,
            List<ProfileRepository.TrackRow> trackRows
    ) {
        List<String> badges = new ArrayList<>();
        badges.add(streak > 0 ? streak + "天连续学习" : "今日待开张");
        if (knownVocabulary >= 50) {
            badges.add("CET 词汇探索者");
        } else if (vocabularyEvents > 0) {
            badges.add("CET 词汇启动");
        }
        if (correctAnswers >= 100) {
            badges.add("百题正确");
        } else if (correctAnswers > 0) {
            badges.add("答题手感回来了");
        }
        trackRows.stream()
                .filter(row -> row.practiced() > 0)
                .findFirst()
                .ifPresent(row -> badges.add(row.name() + "推进中"));
        badges.add(totalEvents > 0 ? "真实数据已连接" : "等待第一条数据");
        return badges.stream().distinct().limit(5).toList();
    }

    private int currentStreak() {
        LocalDate today = LocalDate.now();
        Set<LocalDate> activeDates = new HashSet<>(
                profileRepository.findActiveDates(DEFAULT_USER_ID, today.minusDays(365))
        );
        LocalDate cursor = today;
        if (!activeDates.contains(cursor) && activeDates.contains(cursor.minusDays(1))) {
            cursor = cursor.minusDays(1);
        }
        int streak = 0;
        while (activeDates.contains(cursor)) {
            streak += 1;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private ProfileRecentActivityResponse toRecentActivity(ProfileRepository.RecentEventRow row) {
        String setTitle = clean(row.setTitle(), "题库");
        String questionType = questionTypeLabel(row.questionType());
        String time = timeLabel(row.createdAt());
        String stem = shortText(row.stem(), 18);
        return switch (clean(row.eventType(), "answer")) {
            case "vocabulary" -> new ProfileRecentActivityResponse(
                    "背诵「" + setTitle + "」词汇" + (stem.isBlank() ? "" : "：" + stem),
                    time + " · 标记为" + vocabularyStatusLabel(row.vocabularyStatus())
            );
            case "reveal" -> new ProfileRecentActivityResponse(
                    "查看「" + setTitle + "」参考答案",
                    time + " · " + questionType
            );
            default -> new ProfileRecentActivityResponse(
                    "完成「" + setTitle + "」" + questionType + "练习",
                    time + " · " + answerResultLabel(row.correct()) + answerSelection(row.selectedAnswer())
            );
        };
    }

    private ProfileLearningEventRequest normalizeRequest(ProfileLearningEventRequest request) {
        ProfileLearningEventRequest safeRequest = request == null
                ? new ProfileLearningEventRequest("answer", null, null, null, null, null, null, null)
                : request;
        String eventType = clean(safeRequest.eventType(), "answer").toLowerCase(Locale.ROOT);
        if (!List.of("answer", "reveal", "vocabulary").contains(eventType)) {
            eventType = "answer";
        }
        return new ProfileLearningEventRequest(
                eventType,
                clean(safeRequest.setCode(), null, 64),
                safeRequest.questionId() == null || safeRequest.questionId() <= 0 ? null : safeRequest.questionId(),
                clean(safeRequest.questionType(), null, 32),
                clean(safeRequest.selectedAnswer(), null),
                clean(safeRequest.correctAnswer(), null),
                safeRequest.isCorrect(),
                normalizeVocabularyStatus(safeRequest.vocabularyStatus())
        );
    }

    private String resolveAvatarExtension(MultipartFile file) {
        String contentType = clean(file.getContentType(), "").toLowerCase(Locale.ROOT);
        return switch (contentType) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> resolveAvatarExtensionFromName(file.getOriginalFilename());
        };
    }

    private String resolveAvatarExtensionFromName(String originalFilename) {
        String fileName = clean(originalFilename, "").toLowerCase(Locale.ROOT);
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex >= 0 ? fileName.substring(dotIndex + 1) : "";
        if (List.of("jpg", "jpeg", "png", "webp").contains(extension)) {
            return "jpeg".equals(extension) ? "jpg" : extension;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 jpg、png、webp 头像");
    }

    private Path resolveStoragePath() {
        Path currentDirectory = Path.of("").toAbsolutePath();
        Path directStorage = currentDirectory.resolve("storage").normalize();
        if (Files.isDirectory(directStorage)) {
            return directStorage;
        }
        Path backendStorage = currentDirectory.resolve("StudyPlatform-back").resolve("storage").normalize();
        if (Files.isDirectory(backendStorage)) {
            return backendStorage;
        }
        return directStorage;
    }

    private String normalizeVocabularyStatus(String status) {
        String normalized = clean(status, null, 32);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        return List.of("known", "fuzzy", "unknown").contains(normalized) ? normalized : null;
    }

    private int percentage(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min(100, Math.round(value * 100.0 / total));
    }

    private int activityLevel(int count) {
        if (count <= 0) {
            return 0;
        }
        if (count == 1) {
            return 1;
        }
        if (count <= 4) {
            return 2;
        }
        if (count <= 9) {
            return 3;
        }
        return 4;
    }

    private String toneForCategory(String code) {
        return switch (clean(code, "")) {
            case "english" -> "cyan";
            case "public" -> "blue";
            case "computer" -> "violet";
            case "qualification" -> "amber";
            default -> "cyan";
        };
    }

    private String questionTypeLabel(String type) {
        return switch (clean(type, "")) {
            case "single" -> "单选题";
            case "multiple" -> "多选题";
            case "vocabulary" -> "词汇卡片";
            case "short" -> "主观题";
            default -> "题目";
        };
    }

    private String vocabularyStatusLabel(String status) {
        return switch (clean(status, "")) {
            case "known" -> "认识";
            case "fuzzy" -> "模糊";
            case "unknown" -> "不认识";
            default -> "未标记";
        };
    }

    private String answerResultLabel(Boolean correct) {
        if (correct == null) {
            return "已作答";
        }
        return correct ? "回答正确" : "回答错误";
    }

    private String answerSelection(String selectedAnswer) {
        String selected = clean(selectedAnswer, null);
        return selected == null ? "" : " · 选 " + selected;
    }

    private String timeLabel(LocalDateTime dateTime) {
        LocalDate eventDate = dateTime.toLocalDate();
        LocalDate today = LocalDate.now();
        if (eventDate.equals(today)) {
            return "今日 " + dateTime.format(TIME_FORMATTER);
        }
        if (eventDate.equals(today.minusDays(1))) {
            return "昨日 " + dateTime.format(TIME_FORMATTER);
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    private String shortText(String text, int maxLength) {
        String value = clean(text, "");
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private String formatNumber(long value) {
        return String.format(Locale.CHINA, "%,d", value);
    }

    private String formatDuration(long seconds) {
        long safeSeconds = Math.max(0, seconds);
        long hours = safeSeconds / 3600;
        long minutes = (safeSeconds % 3600) / 60;
        if (hours <= 0) {
            return minutes + "m";
        }
        return hours + "h " + minutes + "m";
    }

    private String formatDurationSeconds(double seconds) {
        return formatDuration(Math.round(seconds));
    }

    private String formatDecimal(double value) {
        return String.format(Locale.CHINA, "%.1f", Math.max(0D, value));
    }

    private String tripleMetricMeta(String total, String best, String average) {
        return "总和 " + total + " · 最佳 " + best + " · 平均 " + average;
    }

    private String clean(String value, String fallback) {
        return clean(value, fallback, 0);
    }

    private String clean(String value, String fallback, int maxLength) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        if (maxLength > 0 && trimmed.length() > maxLength) {
            return trimmed.substring(0, maxLength);
        }
        return trimmed;
    }
}
