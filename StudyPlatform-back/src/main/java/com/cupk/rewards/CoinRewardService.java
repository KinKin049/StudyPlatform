package com.cupk.rewards;

import com.cupk.academy.dto.ProfileLearningEventRequest;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * 金币奖励服务。
 * 提供学习时长奖励、学习行为奖励、游戏奖励及金币消费功能。
 */
@Service
public class CoinRewardService {
    private static final int REWARD_UNIT_SECONDS = 10 * 60;
    private static final int LEARNING_TIME_REWARD_MULTIPLIER = 10;

    private final CoinRewardRepository coinRewardRepository;

    /**
     * 构造函数。
     * @param coinRewardRepository 金币奖励数据访问层
     */
    public CoinRewardService(CoinRewardRepository coinRewardRepository) {
        this.coinRewardRepository = coinRewardRepository;
    }

    /**
     * 根据学习时长发放金币奖励。
     * @param userId 用户ID
     * @param recordId 学习记录ID
     * @param moduleType 学习模块类型
     * @param targetTitle 学习目标标题
     * @param durationSeconds 学习时长（秒）
     */
    public void rewardLearningTime(
            long userId,
            long recordId,
            String moduleType,
            String targetTitle,
            int durationSeconds
    ) {
        int amount = (durationSeconds / REWARD_UNIT_SECONDS)
                * learningTimeRate(moduleType)
                * LEARNING_TIME_REWARD_MULTIPLIER;
        coinRewardRepository.insertReward(
                userId,
                "learning_time",
                "learning-time:" + recordId,
                learningTimeReason(moduleType, targetTitle),
                amount,
                recordId
        );
    }

    /**
     * 根据学习行为发放金币奖励。
     * @param userId 用户ID
     * @param eventId 行为事件ID
     * @param request 学习事件请求
     */
    public void rewardLearningEvent(long userId, long eventId, ProfileLearningEventRequest request) {
        int amount = learningEventAmount(request);
        coinRewardRepository.insertReward(
                userId,
                "learning_event",
                "learning-event:" + eventId,
                learningEventReason(request),
                amount,
                eventId
        );
    }

    /**
     * 万题天梯跳游戏奖励。
     * @param userId 用户ID
     * @param recordId 游戏记录ID
     * @param totalCoins 获得的金币总数
     */
    public void rewardLadderJump(long userId, long recordId, Integer totalCoins) {
        coinRewardRepository.insertReward(
                userId,
                "game",
                "ladder-jump:" + recordId,
                "万题天梯跳游戏获得",
                Math.max(0, totalCoins == null ? 0 : totalCoins),
                recordId
        );
    }

    /**
     * Type Warrior 游戏分数兑换金币。
     * @param userId 用户ID
     * @param recordId 游戏记录ID
     * @param score 游戏分数
     */
    public void rewardTypeWarrior(long userId, long recordId, Long score) {
        int amount = (int) Math.max(0L, Math.round((score == null ? 0L : score) / 100.0));
        coinRewardRepository.insertReward(
                userId,
                "game",
                "type-warrior:" + recordId,
                "Type Warrior 分数兑换",
                amount,
                recordId
        );
    }

    /**
     * 查询用户金币总数。
     * @param userId 用户ID
     * @return 用户金币总数
     */
    public long totalCoins(long userId) {
        return coinRewardRepository.sumRewards(userId);
    }

    /**
     * 消费金币。
     * @param userId 用户ID
     * @param sourceType 消费来源类型
     * @param sourceKey 消费来源标识
     * @param reason 消费原因
     * @param amount 消费数量
     * @param referenceId 关联ID
     * @return 消费成功返回true，否则返回false
     */
    public boolean spendCoins(
            long userId,
            String sourceType,
            String sourceKey,
            String reason,
            int amount,
            Long referenceId
    ) {
        return coinRewardRepository.insertSpend(userId, sourceType, sourceKey, reason, amount, referenceId);
    }

    /**
     * 获取学习时长奖励倍率。
     * @param moduleType 学习模块类型
     * @return 奖励倍率
     */
    private int learningTimeRate(String moduleType) {
        return switch (normalize(moduleType)) {
            case "video" -> 5;
            case "visualization" -> 3;
            case "petroleum" -> 4;
            case "question", "question_bank", "mistake", "favorite", "oj", "assignment", "exam" -> 2;
            default -> 1;
        };
    }

    /**
     * 计算学习行为奖励金额。
     * @param request 学习事件请求
     * @return 奖励金额
     */
    private int learningEventAmount(ProfileLearningEventRequest request) {
        if (request == null) {
            return 0;
        }
        String eventType = normalize(request.eventType());
        if ("answer".equals(eventType) && Boolean.TRUE.equals(request.isCorrect())) {
            return switch (normalize(request.questionType())) {
                case "multiple", "short" -> 2;
                default -> 1;
            };
        }
        if ("vocabulary".equals(eventType) && "known".equals(normalize(request.vocabularyStatus()))) {
            return 1;
        }
        return 0;
    }

    /**
     * 生成学习时长奖励原因描述。
     * @param moduleType 学习模块类型
     * @param targetTitle 学习目标标题
     * @return 奖励原因描述
     */
    private String learningTimeReason(String moduleType, String targetTitle) {
        String target = targetTitle == null || targetTitle.isBlank() ? "" : "：" + targetTitle.trim();
        return switch (normalize(moduleType)) {
            case "video" -> "在线课程学习时长" + target;
            case "visualization" -> "可视化学习在线时长" + target;
            case "petroleum" -> "实验平台在线时长" + target;
            case "oj" -> "OJ 平台练习时长" + target;
            case "question_bank", "question" -> "题库练习时长" + target;
            case "mistake" -> "错题本复习时长" + target;
            case "favorite" -> "收藏题目复习时长" + target;
            case "assignment" -> "作业练习时长" + target;
            case "exam" -> "考试练习时长" + target;
            default -> "学习在线时长" + target;
        };
    }

    private String learningEventReason(ProfileLearningEventRequest request) {
        if (request == null) {
            return "学习行为奖励";
        }
        if ("vocabulary".equals(normalize(request.eventType()))) {
            return "单词掌握奖励";
        }
        return "题目答对奖励";
    }

    /**
     * 标准化字符串，去除首尾空格并转为小写。
     * @param value 原始字符串
     * @return 标准化后的字符串
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
