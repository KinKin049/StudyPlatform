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
 * 为万题天梯跳提供题目池。
 * 支持按题库筛选，保持错题优先，同时在每次拉题时随机打乱顺序。
 */
@Service
public class LadderJumpQuestionService {
    private static final long DEFAULT_USER_ID = 1L;
    private static final Pattern OPTION_PREFIX_PATTERN = Pattern.compile("^[A-Da-d][\\\\.、:：)）\\s]+");

    private final QuestionBankRepository questionBankRepository;

    public LadderJumpQuestionService(QuestionBankRepository questionBankRepository) {
        this.questionBankRepository = questionBankRepository;
    }

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

    public List<LadderJumpQuestionResponse> listQuestions(String setCode) {
        String normalizedSetCode = normalizeSetCode(setCode);

        List<CourseQuestionBankQuestionResponse> mistakeQuestions = new ArrayList<>(
                questionBankRepository.findActiveSingleChoiceMistakeQuestions(DEFAULT_USER_ID, normalizedSetCode)
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

    private String normalizeSetCode(String setCode) {
        if (setCode == null) {
            return null;
        }
        String normalized = setCode.trim();
        return normalized.isBlank() ? null : normalized;
    }

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

    private String sanitizeOptionText(String option) {
        return OPTION_PREFIX_PATTERN.matcher(option).replaceFirst("").trim();
    }

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
