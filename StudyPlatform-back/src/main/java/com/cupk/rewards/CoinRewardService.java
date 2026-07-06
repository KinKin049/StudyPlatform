package com.cupk.rewards;

import com.cupk.academy.dto.ProfileLearningEventRequest;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class CoinRewardService {
    private static final int REWARD_UNIT_SECONDS = 10 * 60;

    private final CoinRewardRepository coinRewardRepository;

    public CoinRewardService(CoinRewardRepository coinRewardRepository) {
        this.coinRewardRepository = coinRewardRepository;
    }

    public void rewardLearningTime(
            long userId,
            long recordId,
            String moduleType,
            String targetTitle,
            int durationSeconds
    ) {
        int amount = (durationSeconds / REWARD_UNIT_SECONDS) * learningTimeRate(moduleType);
        coinRewardRepository.insertReward(
                userId,
                "learning_time",
                "learning-time:" + recordId,
                learningTimeReason(moduleType, targetTitle),
                amount,
                recordId
        );
    }

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

    public long totalCoins(long userId) {
        return coinRewardRepository.sumRewards(userId);
    }

    private int learningTimeRate(String moduleType) {
        return switch (normalize(moduleType)) {
            case "video" -> 5;
            case "visualization" -> 3;
            case "petroleum" -> 4;
            case "question", "question_bank", "mistake", "favorite", "oj", "assignment", "exam" -> 2;
            default -> 1;
        };
    }

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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
