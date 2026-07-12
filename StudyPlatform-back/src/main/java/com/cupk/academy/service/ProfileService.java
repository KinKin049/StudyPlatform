package com.cupk.academy.service;

import com.cupk.academy.dto.ProfileActivityDayResponse;
import com.cupk.academy.dto.ProfileCodingDifficultyResponse;
import com.cupk.academy.dto.ProfileDifficultyResponse;
import com.cupk.academy.dto.ProfileLearningEventRequest;
import com.cupk.academy.dto.ProfileLearningTimeResponse;
import com.cupk.academy.dto.ProfileLearningTimeRecordRequest;
import com.cupk.academy.dto.ProfileOverviewResponse;
import com.cupk.academy.dto.ProfilePreviewMetricResponse;
import com.cupk.academy.dto.ProfileRecentActivityResponse;
import com.cupk.academy.dto.ProfileStatResponse;
import com.cupk.academy.dto.ProfileTrackResponse;
import com.cupk.academy.dto.ProfileUserResponse;
import com.cupk.academy.dto.ProfileUserUpdateRequest;
import com.cupk.academy.dto.QuestionBankMistakeSummaryResponse;
import com.cupk.academy.repository.ProfileRepository;
import com.cupk.academy.repository.QuestionBankRepository;
import com.cupk.auth.repository.AuthUserRepository;
import com.cupk.auth.repository.AuthUserRepository.AuthUserRow;
import com.cupk.config.FileSignatureValidator;
import com.cupk.config.StoragePathProvider;
import com.cupk.games.repository.GameRecordRepository;
import com.cupk.rewards.CoinRewardService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 用户档案服务，提供学习统计、个人资料管理、成就徽章等功能。
 */
@Service
public class ProfileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileService.class);
    private static final int HEATMAP_DAYS = 119;
    private static final long MAX_AVATAR_SIZE = 2L * 1024L * 1024L;
    private static final int MAX_LEARNING_TIME_SECONDS = 12 * 60 * 60;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final ProfileRepository profileRepository;
    private final GameRecordRepository gameRecordRepository;
    private final QuestionBankRepository questionBankRepository;
    private final CoinRewardService coinRewardService;
    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final StoragePathProvider storagePathProvider;

    /**
     * 构造函数，注入依赖的仓库和服务。
     *
     * @param profileRepository      用户档案数据访问层
     * @param gameRecordRepository   游戏记录数据访问层
     * @param questionBankRepository 题库数据访问层
     * @param coinRewardService      金币奖励服务
     * @param authUserRepository     认证用户数据访问层
     * @param passwordEncoder        密码编码器
     */
    public ProfileService(
            ProfileRepository profileRepository,
            GameRecordRepository gameRecordRepository,
            QuestionBankRepository questionBankRepository,
            CoinRewardService coinRewardService,
            AuthUserRepository authUserRepository,
            PasswordEncoder passwordEncoder,
            StoragePathProvider storagePathProvider
    ) {
        this.profileRepository = profileRepository;
        this.gameRecordRepository = gameRecordRepository;
        this.questionBankRepository = questionBankRepository;
        this.coinRewardService = coinRewardService;
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.storagePathProvider = storagePathProvider;
    }

    /**
     * 获取用户学习概览数据，包含统计信息、进度分布、活动热力图、游戏数据等。
     *
     * @param userId 用户ID
     * @return 学习概览响应对象
     */
    public ProfileOverviewResponse getOverview(long userId) {
        long totalEvents = profileRepository.countEvents(userId);
        long correctAnswers = profileRepository.countCorrectAnswers(userId);
        long knownVocabulary = profileRepository.countKnownVocabulary(userId);
        long vocabularyEvents = profileRepository.countVocabularyEvents(userId);
        long practicedQuestions = profileRepository.countDistinctPracticedQuestions(userId);
        long totalQuestions = profileRepository.countTotalQuestions();
        LocalDate today = LocalDate.now();
        long totalLearningSeconds = profileRepository.sumAllLearningTimeSeconds(userId);
        long todayLearningSeconds = profileRepository.sumLearningTimeSecondsBetween(
                userId,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
        int overallProgress = percentage(practicedQuestions, totalQuestions);
        int streak = currentStreak(userId);

        List<ProfileRepository.TrackRow> trackRows = profileRepository.findTrackRows(userId);
        GameRecordRepository.LadderJumpAggregateRow ladderJumpAggregate =
                gameRecordRepository.findLadderJumpAggregate(userId);
        GameRecordRepository.TypeWarriorAggregateRow typeWarriorAggregate =
                gameRecordRepository.findTypeWarriorAggregate(userId);
        long adminCoinAdjustment = profileRepository.findAdminCoinAdjustment(userId);

        return new ProfileOverviewResponse(
                List.of(
                        new ProfileStatResponse("学习时长", formatDuration(totalLearningSeconds), "真实累计时长"),
                        new ProfileStatResponse("今日学习", formatDuration(todayLearningSeconds), "今日真实时长"),
                        new ProfileStatResponse("练习记录", formatNumber(totalEvents), "答题 / 查答案 / 背词"),
                        new ProfileStatResponse("连续学习", String.valueOf(streak), "天")
                ),
                overallProgress,
                buildDistribution(userId),
                buildTracks(trackRows),
                buildRecentActivities(userId),
                buildBadges(streak, totalEvents, correctAnswers, knownVocabulary, vocabularyEvents, trackRows),
                buildHeatmap(userId),
                buildLearningTimes(userId),
                buildCodingDifficulties(userId),
                buildGameMetrics(userId, ladderJumpAggregate, typeWarriorAggregate),
                coinRewardService.totalCoins(userId) + adminCoinAdjustment,
                buildMistakeMetrics(userId),
                buildRankingMetrics(userId, totalLearningSeconds),
                buildAchievementMetrics(),
                buildTextbookOrders(userId)
        );
    }

    /**
     * 记录学习事件，包括答题、查看答案、背词等操作，并触发金币奖励。
     *
     * @param userId  用户ID
     * @param request 学习事件请求对象
     */
    public void recordLearningEvent(long userId, ProfileLearningEventRequest request) {
        ProfileLearningEventRequest normalizedRequest = normalizeRequest(request);
        long eventId = profileRepository.insertLearningEvent(userId, normalizedRequest);
        coinRewardService.rewardLearningEvent(userId, eventId, normalizedRequest);
    }

    /**
     * 记录学习时长，支持不同模块类型的时长统计，并触发金币奖励。
     *
     * @param userId  用户ID
     * @param request 学习时长记录请求对象
     */
    public void recordLearningTime(long userId, ProfileLearningTimeRecordRequest request) {
        if (request == null || request.durationSeconds() == null) {
            return;
        }
        int durationSeconds = Math.min(Math.max(request.durationSeconds(), 0), MAX_LEARNING_TIME_SECONDS);
        if (durationSeconds <= 0) {
            return;
        }
        String moduleType = normalizeLearningModuleType(request.moduleType());
        String targetTitle = clean(request.targetTitle(), null, 128);
        long recordId = profileRepository.insertLearningTimeRecord(
                userId,
                moduleType,
                clean(request.targetCode(), null, 128),
                targetTitle,
                durationSeconds
        );
        coinRewardService.rewardLearningTime(userId, recordId, moduleType, targetTitle, durationSeconds);
    }

    /**
     * 获取用户个人资料信息。
     *
     * @param userId 用户ID
     * @return 用户资料响应对象
     */
    public ProfileUserResponse getUserProfile(long userId) {
        return profileRepository.findUserProfile(userId);
    }

    /**
     * 更新用户个人资料，包括昵称、邮箱、简介、位置、标签和密码。
     *
     * @param userId  用户ID
     * @param request 用户资料更新请求对象
     * @return 更新后的用户资料响应对象
     */
    public ProfileUserResponse updateUserProfile(long userId, ProfileUserUpdateRequest request) {
        ProfileUserResponse currentProfile = getUserProfile(userId);
        String name = clean(request == null ? null : request.name(), currentProfile.name(), 64);
        String email = clean(request == null ? null : request.email(), currentProfile.email(), 128)
                .toLowerCase(Locale.ROOT);
        String bio = request == null || request.bio() == null
                ? currentProfile.bio()
                : clean(request.bio(), "", 512);
        String location = clean(request == null ? null : request.location(), currentProfile.location(), 64);
        List<String> metaTags = cleanMetaTags(
                request == null ? null : request.metaTags(),
                currentProfile.metaTags()
        );
        if (name == null || name.isBlank()) {
            LOGGER.warn("Profile update rejected: blank display name, userId={}", userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "昵称不能为空");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            LOGGER.warn("Profile update rejected: invalid email, userId={}", userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱格式不正确");
        }
        if (authUserRepository.emailBelongsToOtherUser(email, userId)) {
            LOGGER.warn("Profile update rejected: duplicate email, userId={}, email={}", userId, email);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱已被其他账号使用");
        }

        updatePasswordIfRequested(userId, request);
        authUserRepository.updateEmail(userId, email);
        profileRepository.updateUserProfile(userId, name, bio, location, metaTags);
        LOGGER.info("Profile user updated successfully: userId={}, displayName={}, email={}, bioLength={}",
                userId, name, email, bio == null ? 0 : bio.length());
        return getUserProfile(userId);
    }

    private void updatePasswordIfRequested(long userId, ProfileUserUpdateRequest request) {
        String currentPassword = request == null || request.currentPassword() == null
                ? ""
                : request.currentPassword();
        String newPassword = request == null || request.newPassword() == null
                ? ""
                : request.newPassword();
        String confirmNewPassword = request == null || request.confirmNewPassword() == null
                ? ""
                : request.confirmNewPassword();
        boolean passwordTouched = !currentPassword.isBlank()
                || !newPassword.isBlank()
                || !confirmNewPassword.isBlank();
        if (!passwordTouched) {
            return;
        }
        if (currentPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写旧密码");
        }
        if (newPassword.length() < 6 || newPassword.length() > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码长度需为 6-72 位");
        }
        if (!newPassword.equals(confirmNewPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "两次输入的新密码不一致");
        }
        AuthUserRow authUser = authUserRepository.findById(userId);
        if (authUser == null || !passwordEncoder.matches(currentPassword, authUser.passwordHash())) {
            LOGGER.warn("Profile password update rejected: old password mismatch, userId={}", userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "旧密码不正确");
        }
        authUserRepository.updatePassword(userId, passwordEncoder.encode(newPassword));
    }

    private List<String> cleanMetaTags(List<String> tags, List<String> fallback) {
        List<String> source = tags == null ? fallback : tags;
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        if (source != null) {
            for (String tag : source) {
                String value = clean(tag, "", 32);
                if (!value.isBlank()) {
                    cleaned.add(value);
                }
                if (cleaned.size() >= 12) {
                    break;
                }
            }
        }
        return new ArrayList<>(cleaned);
    }

    /**
     * 更新用户头像，支持 jpg、png、webp 格式，最大 2MB。
     *
     * @param userId 用户ID
     * @param file   头像图片文件
     * @return 更新后的用户资料响应对象
     */
    public ProfileUserResponse updateAvatar(long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            LOGGER.warn("Profile avatar update rejected: empty file, userId={}", userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择头像图片");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            LOGGER.warn("Profile avatar update rejected: file too large, userId={}, fileSize={}",
                    userId, file.getSize());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像图片不能超过 2MB");
        }
        String extension = FileSignatureValidator.requireImage(file, List.of("jpg", "png", "webp"));
        String fileName = "avatar-" + userId + "-" + UUID.randomUUID() + "." + extension;
        Path avatarDirectory = storagePathProvider.storageRoot().resolve("profile").resolve("avatars").normalize();
        Path targetPath = avatarDirectory.resolve(fileName).normalize();
        if (!targetPath.startsWith(avatarDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像文件名不合法");
        }
        try {
            Files.createDirectories(avatarDirectory);
            file.transferTo(targetPath);
        } catch (IOException ex) {
            LOGGER.error("Profile avatar save failed: userId={}, targetPath={}", userId, targetPath, ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "头像保存失败", ex);
        }
        profileRepository.updateAvatarPath(userId, "profile/avatars/" + fileName);
        LOGGER.info("Profile avatar updated successfully: userId={}, fileName={}, fileSize={}",
                userId, fileName, file.getSize());
        return getUserProfile(userId);
    }

    /**
     * 构建题目难度分布数据。
     *
     * @param userId 用户ID
     * @return 难度分布响应列表
     */
    private List<ProfileDifficultyResponse> buildDistribution(long userId) {
        return profileRepository.findDistributionRows(userId).stream()
                .filter(row -> row.total() > 0 || row.solved() > 0)
                .map(row -> new ProfileDifficultyResponse(row.label(), row.solved(), row.total(), row.color()))
                .toList();
    }

    /**
     * 构建学习轨迹数据。
     *
     * @param trackRows 轨迹行数据列表
     * @return 轨迹响应列表
     */
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

    /**
     * 构建最近学习活动记录。
     *
     * @param userId 用户ID
     * @return 最近活动响应列表
     */
    private List<ProfileRecentActivityResponse> buildRecentActivities(long userId) {
        List<ProfileRecentActivityResponse> activities = profileRepository.findRecentEvents(userId, 6).stream()
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

    /**
     * 构建学习活动热力图数据，统计最近119天的每日活动次数。
     *
     * @param userId 用户ID
     * @return 活动天数响应列表
     */
    private List<ProfileActivityDayResponse> buildHeatmap(long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(HEATMAP_DAYS - 1L);
        Map<LocalDate, Integer> counts = profileRepository.countEventsByDate(userId, startDate, today);
        List<ProfileActivityDayResponse> days = new ArrayList<>();
        for (int index = 0; index < HEATMAP_DAYS; index += 1) {
            LocalDate date = startDate.plusDays(index);
            int count = counts.getOrDefault(date, 0);
            days.add(new ProfileActivityDayResponse(index, date.toString(), count, activityLevel(count)));
        }
        return days;
    }

    /**
     * 构建学习时长统计数据。
     *
     * @param userId 用户ID
     * @return 学习时长响应列表
     */
    private List<ProfileLearningTimeResponse> buildLearningTimes(long userId) {
        long totalSeconds = profileRepository.sumAllLearningTimeSeconds(userId);
        long visualizationSeconds = profileRepository.sumLearningTimeSeconds(userId, "visualization");
        return List.of(
                new ProfileLearningTimeResponse(
                        "学习时长",
                        formatDuration(totalSeconds),
                        totalSeconds > 0 ? "题库、课程、OJ、可视化和油气仿真累计" : "暂无学习时长记录",
                        "cyan"
                ),
                new ProfileLearningTimeResponse(
                        "可视化时长",
                        formatDuration(visualizationSeconds),
                        visualizationSeconds > 0 ? "仅统计可视化具体页面" : "暂无可视化时长记录",
                        "violet"
                )
        );
    }

    /**
     * 构建编程难度统计数据，包括简单、中等、困难三个级别。
     *
     * @param userId 用户ID
     * @return 编程难度响应列表
     */
    private List<ProfileCodingDifficultyResponse> buildCodingDifficulties(long userId) {
        Map<String, ProfileRepository.CodingDifficultyRow> rows = profileRepository.findCodingDifficultyRows(userId)
                .stream()
                .collect(java.util.stream.Collectors.toMap(ProfileRepository.CodingDifficultyRow::difficulty, row -> row));
        return List.of(
                codingDifficulty("简单", "EASY", "#00b8a3", rows.get("EASY")),
                codingDifficulty("中等", "MEDIUM", "#ffc01e", rows.get("MEDIUM")),
                codingDifficulty("困难", "HARD", "#ef476f", rows.get("HARD"))
        );
    }

    /**
     * 创建编程难度响应对象。
     *
     * @param label 显示标签
     * @param level 难度级别
     * @param color 颜色值
     * @param row   数据库查询行
     * @return 编程难度响应对象
     */
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

    /**
     * 构建游戏统计数据，包括天梯跳和Type Warrior的各项指标。
     *
     * @param userId       用户ID
     * @param ladderJump   天梯跳聚合数据
     * @param typeWarrior  Type Warrior聚合数据
     * @return 游戏指标响应列表
     */
    private List<ProfilePreviewMetricResponse> buildGameMetrics(
            long userId,
            GameRecordRepository.LadderJumpAggregateRow ladderJump,
            GameRecordRepository.TypeWarriorAggregateRow typeWarrior
    ) {
        GameRecordRepository.CombinedDurationAggregateRow combinedDuration =
                gameRecordRepository.findCombinedDurationAggregate(userId);

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

    /**
     * 构建错题统计数据。
     *
     * @param userId 用户ID
     * @return 错题指标响应列表
     */
    private List<ProfilePreviewMetricResponse> buildMistakeMetrics(long userId) {
        QuestionBankMistakeSummaryResponse summary = questionBankRepository.findMistakeSummary(userId);
        long setCount = summary.sets() == null ? 0 : summary.sets().size();
        return List.of(
                new ProfilePreviewMetricResponse(
                        "错题本",
                        formatNumber(summary.total()) + " 题",
                        "待复习 " + formatNumber(summary.active()) + " 题 · 已掌握 " + formatNumber(summary.mastered()) + " 题",
                        "rose"
                ),
                new ProfilePreviewMetricResponse(
                        "薄弱题库",
                        formatNumber(setCount) + " 个",
                        setCount > 0 ? "来自当前账号的错题记录" : "当前账号暂无错题记录",
                        "amber"
                )
        );
    }

    /**
     * 构建排名统计数据。
     *
     * @return 排名指标响应列表
     */
    private List<ProfilePreviewMetricResponse> buildRankingMetrics(long userId, long totalLearningSeconds) {
        ProfileRepository.LearningTimeRankingRow ranking = profileRepository.findLearningTimeRanking(userId);
        long participantCount = Math.max(ranking.participantCount(), 1);
        long rank = Math.max(ranking.rank(), 1);
        long exceededPercent = participantCount <= 1
                ? 0
                : Math.max(0, Math.round((participantCount - rank) * 100.0 / participantCount));
        return List.of(
                new ProfilePreviewMetricResponse(
                        "学习时长排名",
                        "#" + formatNumber(rank),
                        "累计学习时长 " + formatDuration(totalLearningSeconds)
                                + " · 共 " + formatNumber(participantCount) + " 名学习者",
                        "cyan"
                ),
                new ProfilePreviewMetricResponse(
                        "累计学习时长",
                        formatDuration(totalLearningSeconds),
                        "排名依据：全站累计学习时长 · 超过 " + exceededPercent + "% 学习者",
                        "blue"
                )
        );
    }

    /**
     * 构建成就统计预览数据。
     *
     * @return 成就指标响应列表
     */
    private List<ProfilePreviewMetricResponse> buildAchievementMetrics() {
        return List.of(
                new ProfilePreviewMetricResponse("成就点数", "0", "暂无成就点数", "violet"),
                new ProfilePreviewMetricResponse("稀有成就", "0 枚", "暂无稀有成就", "amber")
        );
    }

    /**
     * 构建教材订单统计数据。
     *
     * @return 教材订单指标响应列表
     */
    private List<ProfilePreviewMetricResponse> buildTextbookOrders(long userId) {
        ProfileRepository.TextbookProfileStatsRow stats = profileRepository.findTextbookProfileStats(userId);
        return List.of(
                new ProfilePreviewMetricResponse(
                        "购物车",
                        formatNumber(stats.cartQuantity()) + " 本",
                        stats.cartItemCount() > 0
                                ? "共 " + formatNumber(stats.cartItemCount()) + " 种教材待结算"
                                : "购物车暂无教材",
                        "blue"
                ),
                new ProfilePreviewMetricResponse(
                        "待评价",
                        formatNumber(stats.pendingReviewCount()) + " 本",
                        stats.pendingReviewCount() > 0
                                ? "已购教材中还有内容等待评价"
                                : "已购教材均已评价或暂无已购教材",
                        "amber"
                )
        );
    }

    /**
     * 根据用户学习数据构建成就徽章列表。
     *
     * @param streak          连续学习天数
     * @param totalEvents     总学习事件数
     * @param correctAnswers  正确答题数
     * @param knownVocabulary 已掌握词汇数
     * @param vocabularyEvents 词汇学习事件数
     * @param trackRows       轨迹行数据列表
     * @return 徽章名称列表
     */
    private List<String> buildBadges(
            int streak,
            long totalEvents,
            long correctAnswers,
            long knownVocabulary,
            long vocabularyEvents,
            List<ProfileRepository.TrackRow> trackRows
    ) {
        List<String> badges = new ArrayList<>();
        if (streak > 0) {
            badges.add(streak + "天连续学习");
        }
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
        if (totalEvents > 0) {
            badges.add("真实数据已连接");
        }
        return badges.stream().distinct().limit(5).toList();
    }

    /**
     * 计算用户当前连续学习天数。
     *
     * @param userId 用户ID
     * @return 连续学习天数
     */
    private int currentStreak(long userId) {
        LocalDate today = LocalDate.now();
        Set<LocalDate> activeDates = new HashSet<>(
                profileRepository.findActiveDates(userId, today.minusDays(365))
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

    /**
     * 将最近事件行数据转换为活动响应对象。
     *
     * @param row 最近事件行数据
     * @return 最近活动响应对象
     */
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

    /**
     * 规范化学习事件请求参数。
     *
     * @param request 原始请求对象
     * @return 规范化后的请求对象
     */
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

    /**
     * 规范化学习模块类型。
     *
     * @param moduleType 原始模块类型
     * @return 规范化后的模块类型
     */
    private String normalizeLearningModuleType(String moduleType) {
        String normalized = clean(moduleType, "general", 32).toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "question", "question_bank", "mistake", "favorite", "video", "oj",
                    "visualization", "petroleum", "assignment", "exam" -> normalized;
            default -> "general";
        };
    }

    /**
     * 规范化词汇状态。
     *
     * @param status 原始状态
     * @return 规范化后的状态
     */
    private String normalizeVocabularyStatus(String status) {
        String normalized = clean(status, null, 32);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        return List.of("known", "fuzzy", "unknown").contains(normalized) ? normalized : null;
    }

    /**
     * 计算百分比。
     *
     * @param value  当前值
     * @param total  总值
     * @return 百分比（0-100）
     */
    private int percentage(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min(100, Math.round(value * 100.0 / total));
    }

    /**
     * 根据活动次数计算活动级别（用于热力图颜色）。
     *
     * @param count 活动次数
     * @return 活动级别（0-4）
     */
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

    /**
     * 根据课程分类代码获取主题颜色。
     *
     * @param code 分类代码
     * @return 颜色名称
     */
    private String toneForCategory(String code) {
        return switch (clean(code, "")) {
            case "english" -> "cyan";
            case "public" -> "blue";
            case "computer" -> "violet";
            case "qualification" -> "amber";
            default -> "cyan";
        };
    }

    /**
     * 将题目类型代码转换为中文标签。
     *
     * @param type 类型代码
     * @return 中文标签
     */
    private String questionTypeLabel(String type) {
        return switch (clean(type, "")) {
            case "single" -> "单选题";
            case "multiple" -> "多选题";
            case "vocabulary" -> "词汇卡片";
            case "short" -> "主观题";
            default -> "题目";
        };
    }

    /**
     * 将词汇状态代码转换为中文标签。
     *
     * @param status 状态代码
     * @return 中文标签
     */
    private String vocabularyStatusLabel(String status) {
        return switch (clean(status, "")) {
            case "known" -> "认识";
            case "fuzzy" -> "模糊";
            case "unknown" -> "不认识";
            default -> "未标记";
        };
    }

    /**
     * 将答题结果转换为中文标签。
     *
     * @param correct 是否正确
     * @return 中文标签
     */
    private String answerResultLabel(Boolean correct) {
        if (correct == null) {
            return "已作答";
        }
        return correct ? "回答正确" : "回答错误";
    }

    /**
     * 格式化答题选项显示。
     *
     * @param selectedAnswer 选择的答案
     * @return 格式化后的选项文本
     */
    private String answerSelection(String selectedAnswer) {
        String selected = clean(selectedAnswer, null);
        return selected == null ? "" : " · 选 " + selected;
    }

    /**
     * 格式化时间显示，区分今日、昨日和其他日期。
     *
     * @param dateTime 时间
     * @return 格式化后的时间文本
     */
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

    /**
     * 将文本截断到指定长度，超出部分用省略号表示。
     *
     * @param text      原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    private String shortText(String text, int maxLength) {
        String value = clean(text, "");
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    /**
     * 格式化数字，添加千位分隔符。
     *
     * @param value 数字值
     * @return 格式化后的字符串
     */
    private String formatNumber(long value) {
        return String.format(Locale.CHINA, "%,d", value);
    }

    /**
     * 格式化时长，显示为小时和分钟。
     *
     * @param seconds 秒数
     * @return 格式化后的时长字符串
     */
    private String formatDuration(long seconds) {
        long safeSeconds = Math.max(0, seconds);
        long hours = safeSeconds / 3600;
        long minutes = (safeSeconds % 3600) / 60;
        if (hours <= 0) {
            return minutes + "m";
        }
        return hours + "h " + minutes + "m";
    }

    /**
     * 格式化时长（double类型），显示为小时和分钟。
     *
     * @param seconds 秒数
     * @return 格式化后的时长字符串
     */
    private String formatDurationSeconds(double seconds) {
        return formatDuration(Math.round(seconds));
    }

    /**
     * 格式化小数，保留一位小数。
     *
     * @param value 小数值
     * @return 格式化后的字符串
     */
    private String formatDecimal(double value) {
        return String.format(Locale.CHINA, "%.1f", Math.max(0D, value));
    }

    /**
     * 构建三元指标元数据描述。
     *
     * @param total   总和值
     * @param best    最佳值
     * @param average 平均值
     * @return 格式化后的元数据字符串
     */
    private String tripleMetricMeta(String total, String best, String average) {
        return "总和 " + total + " · 最佳 " + best + " · 平均 " + average;
    }

    /**
     * 清理字符串，去除首尾空格，空值时返回默认值。
     *
     * @param value    原始字符串
     * @param fallback 默认值
     * @return 清理后的字符串
     */
    private String clean(String value, String fallback) {
        return clean(value, fallback, 0);
    }

    /**
     * 清理字符串，去除首尾空格，空值时返回默认值，并可限制最大长度。
     *
     * @param value      原始字符串
     * @param fallback   默认值
     * @param maxLength  最大长度（0表示不限制）
     * @return 清理后的字符串
     */
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
