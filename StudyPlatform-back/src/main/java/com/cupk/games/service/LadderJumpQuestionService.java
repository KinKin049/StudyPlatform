package com.cupk.games.service;

import com.cupk.academy.dto.CourseQuestionBankQuestionResponse;
import com.cupk.academy.repository.QuestionBankRepository;
import com.cupk.games.dto.LadderJumpQuestionBankResponse;
import com.cupk.games.dto.LadderJumpQuestionResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * 万题天梯跳题目服务，为游戏提供题目池。
 * 支持按题库筛选，保持错题优先，同时在每次拉题时随机打乱顺序。
 */
@Service
public class LadderJumpQuestionService {
    private static final Pattern OPTION_PREFIX_PATTERN = Pattern.compile("^[A-Da-d][\\\\.、:：)）\\s]+");

    private final QuestionBankRepository questionBankRepository;

    /**
     * 构造函数，注入题库数据访问层。
     *
     * @param questionBankRepository 题库数据访问层
     */
    public LadderJumpQuestionService(QuestionBankRepository questionBankRepository) {
        this.questionBankRepository = questionBankRepository;
    }

    /**
     * 查询可用的单选题题库列表。
     *
     * @return 题库响应列表
     */
    public List<LadderJumpQuestionBankResponse> listQuestionBanks() {
        return questionBankRepository.findSingleChoiceQuestionBanks().stream()
                .map(row -> new LadderJumpQuestionBankResponse(
                        row.setCode(),
                        row.title(),
                        row.categoryName(),
                        row.questionCount()
                ))
                .toList();
    }

    /**
     * 查询题目列表，错题优先，支持按题库筛选。
     *
     * @param userId   用户ID
     * @param setCode  题库代码（可选）
     * @return 题目响应列表
     */
    public List<LadderJumpQuestionResponse> listQuestions(long userId, String setCode) {
        String normalizedSetCode = normalizeSetCode(setCode);

        List<CourseQuestionBankQuestionResponse> mistakeQuestions = new ArrayList<>(
                questionBankRepository.findActiveSingleChoiceMistakeQuestions(userId, normalizedSetCode)
        );
        List<CourseQuestionBankQuestionResponse> allQuestions = new ArrayList<>(
                questionBankRepository.findAllSingleChoiceQuestions(normalizedSetCode)
        );
        Collections.shuffle(mistakeQuestions);
        Collections.shuffle(allQuestions);

        Map<Long, LadderJumpQuestionResponse> orderedQuestions = new LinkedHashMap<>();
        appendQuestions(orderedQuestions, mistakeQuestions);
        appendQuestions(orderedQuestions, allQuestions);
        if (!orderedQuestions.isEmpty()) {
            return List.copyOf(orderedQuestions.values());
        }
        if (normalizedSetCode != null) {
            return List.of();
        }
        return fallbackQuestions();
    }

    /**
     * 规范化题库代码，空值或空白字符串返回null。
     *
     * @param setCode 原始题库代码
     * @return 规范化后的题库代码
     */
    private String normalizeSetCode(String setCode) {
        if (setCode == null) {
            return null;
        }
        String normalized = setCode.trim();
        return normalized.isBlank() ? null : normalized;
    }

    /**
     * 将题目追加到有序题目映射中，避免重复。
     *
     * @param orderedQuestions 有序题目映射
     * @param sourceQuestions  源题目列表
     */
    private void appendQuestions(
            Map<Long, LadderJumpQuestionResponse> orderedQuestions,
            List<CourseQuestionBankQuestionResponse> sourceQuestions
    ) {
        for (CourseQuestionBankQuestionResponse question : sourceQuestions) {
            if (orderedQuestions.containsKey(question.id())) {
                continue;
            }
            LadderJumpQuestionResponse mappedQuestion = mapQuestion(question);
            if (mappedQuestion != null) {
                orderedQuestions.put(mappedQuestion.id(), mappedQuestion);
            }
        }
    }

    /**
     * 将题库题目转换为游戏题目响应对象。
     *
     * @param question 题库题目
     * @return 游戏题目响应对象，无效题目返回null
     */
    private LadderJumpQuestionResponse mapQuestion(CourseQuestionBankQuestionResponse question) {
        List<String> rawOptions = question.options() == null ? List.of() : question.options().stream()
                .map(option -> option == null ? "" : option.trim())
                .filter(option -> !option.isBlank())
                .toList();
        if (rawOptions.size() < 2) {
            return null;
        }

        List<String> displayOptions = rawOptions.stream()
                .map(this::sanitizeOptionText)
                .toList();
        int answerIndex = resolveAnswerIndex(question.answer(), rawOptions, displayOptions);
        if (answerIndex < 0) {
            return null;
        }

        String explanation = question.explanation();
        if (explanation == null || explanation.isBlank()) {
            explanation = "正确答案：" + displayOptions.get(answerIndex);
        }

        return new LadderJumpQuestionResponse(
                question.id(),
                question.stem(),
                displayOptions,
                answerIndex,
                explanation
        );
    }

    /**
     * 解析正确答案索引。
     *
     * @param answer        答案文本
     * @param rawOptions    原始选项列表
     * @param displayOptions 显示选项列表
     * @return 答案索引，无效返回-1
     */
    private int resolveAnswerIndex(String answer, List<String> rawOptions, List<String> displayOptions) {
        String trimmedAnswer = answer == null ? "" : answer.trim();
        if (trimmedAnswer.isBlank()) {
            return -1;
        }

        char firstChar = Character.toUpperCase(trimmedAnswer.charAt(0));
        if (firstChar >= 'A' && firstChar < 'A' + rawOptions.size()) {
            return firstChar - 'A';
        }

        for (int index = 0; index < rawOptions.size(); index += 1) {
            if (trimmedAnswer.equalsIgnoreCase(rawOptions.get(index))
                    || trimmedAnswer.equalsIgnoreCase(displayOptions.get(index))) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 清理选项文本，移除前缀标记（如A.、B、等）。
     *
     * @param option 原始选项文本
     * @return 清理后的选项文本
     */
    private String sanitizeOptionText(String option) {
        return OPTION_PREFIX_PATTERN.matcher(option).replaceFirst("").trim();
    }

    /**
     * 获取备选题目列表（当无可用题目时使用）。
     *
     * @return 备选题目列表
     */
    private List<LadderJumpQuestionResponse> fallbackQuestions() {
        return List.of(
                new LadderJumpQuestionResponse(
                        1L,
                        "Java 中用于声明类继承关系的关键字是？",
                        List.of("extends", "implements", "instanceof"),
                        0,
                        "extends 用于声明一个类继承另一个类。"
                ),
                new LadderJumpQuestionResponse(
                        2L,
                        "HTTP 状态码 404 通常表示什么？",
                        List.of("请求成功", "资源不存在", "服务器重启"),
                        1,
                        "404 表示客户端请求的资源未找到。"
                ),
                new LadderJumpQuestionResponse(
                        3L,
                        "CSS 中控制定位元素层级顺序的属性是？",
                        List.of("display", "z-index", "overflow"),
                        1,
                        "z-index 用于控制定位元素的层叠顺序。"
                )
        );
    }
}
