package com.cupk.academy.service;

import com.cupk.academy.repository.QuestionBankRepository;
import com.cupk.academy.repository.QuestionBankRepository.CourseQuestionBankQuestionSeed;
import com.cupk.academy.service.QuestionBankSourceResolver.SourceFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MaoismQuestionBankSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MaoismQuestionBankSeeder.class);
    private static final String SET_CODE = "maoism";
    private static final String SOURCE_FILE = "maoism.html";
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final QuestionBankRepository questionBankRepository;
    private final ObjectMapper objectMapper;

    public MaoismQuestionBankSeeder(QuestionBankRepository questionBankRepository, ObjectMapper objectMapper) {
        this.questionBankRepository = questionBankRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        Optional<SourceFile> sourceFile = QuestionBankSourceResolver.find(SOURCE_FILE, log);
        if (sourceFile.isEmpty()) {
            log.warn("Maoism question bank source file not found");
            return;
        }

        try {
            MaoismQuestionBank source = objectMapper.readValue(extractQuestionJson(sourceFile.get()), MaoismQuestionBank.class);
            List<CourseQuestionBankQuestionSeed> questions = toSeeds(source, sourceFile.get().fileName());
            if (questions.isEmpty()) {
                log.warn("Maoism question bank source file {} has no usable questions", sourceFile.get().location());
                return;
            }

            long existing = questionBankRepository.countCourseQuestionBankQuestions(SET_CODE, "single")
                    + questionBankRepository.countCourseQuestionBankQuestions(SET_CODE, "multiple");
            if (existing >= questions.size()) {
                log.info("Maoism question bank already has {} questions, skip seeding", existing);
                return;
            }

            questionBankRepository.deleteCourseQuestionBankQuestions(SET_CODE);
            questionBankRepository.batchInsertCourseQuestionBankQuestions(SET_CODE, questions);
            log.info("Seeded {} maoism questions from {}", questions.size(), sourceFile.get().location());
        } catch (IOException ex) {
            log.warn("Failed to seed maoism question bank from {}", sourceFile.get().location(), ex);
        }
    }

    private String extractQuestionJson(SourceFile sourceFile) throws IOException {
        String html = sourceFile.readString();
        int declarationIndex = html.indexOf("var QUESTION_BANK");
        if (declarationIndex < 0) {
            throw new IOException("QUESTION_BANK declaration not found");
        }
        int objectStart = html.indexOf('{', declarationIndex);
        if (objectStart < 0) {
            throw new IOException("question object start not found");
        }
        int totalTimeIndex = html.indexOf("var TOTAL_TIME", objectStart);
        if (totalTimeIndex < 0) {
            throw new IOException("QUESTION_BANK end marker not found");
        }
        int objectEnd = html.lastIndexOf(';', totalTimeIndex);
        if (objectEnd < objectStart) {
            throw new IOException("question object end not found");
        }
        return html.substring(objectStart, objectEnd).trim();
    }

    private List<CourseQuestionBankQuestionSeed> toSeeds(MaoismQuestionBank source, String sourceName) {
        List<CourseQuestionBankQuestionSeed> questions = new ArrayList<>();
        int sortOrder = 10;
        sortOrder = appendChoiceQuestions(questions, source.s(), "single", "单选题", sourceName, sortOrder);
        sortOrder = appendChoiceQuestions(questions, source.m(), "multiple", "多选题", sourceName, sortOrder);
        appendJudgeQuestions(questions, source.t(), sourceName, sortOrder);
        return questions;
    }

    private int appendChoiceQuestions(
            List<CourseQuestionBankQuestionSeed> questions,
            List<MaoismQuestion> sourceQuestions,
            String type,
            String typeLabel,
            String sourceName,
            int sortOrder
    ) {
        if (sourceQuestions == null) {
            return sortOrder;
        }
        for (MaoismQuestion question : sourceQuestions) {
            List<String> options = normalizeOptions(question == null ? null : question.o());
            String answer = toDelimitedAnswer(normalizeAnswerLetters(question == null ? null : question.a()));
            if (question == null || isBlank(question.q()) || options.isEmpty() || answer.isBlank()) {
                continue;
            }
            String answerText = answerText(options, answer);
            questions.add(new CourseQuestionBankQuestionSeed(
                    type,
                    question.q().trim(),
                    options,
                    answer,
                    buildExplanation(answer, answerText, typeLabel),
                    "基础到综合",
                    sourceName,
                    sortOrder
            ));
            sortOrder += 10;
        }
        return sortOrder;
    }

    private int appendJudgeQuestions(
            List<CourseQuestionBankQuestionSeed> questions,
            List<MaoismQuestion> sourceQuestions,
            String sourceName,
            int sortOrder
    ) {
        if (sourceQuestions == null) {
            return sortOrder;
        }
        for (MaoismQuestion question : sourceQuestions) {
            String answer = normalizeJudgeAnswer(question == null ? null : question.a());
            if (question == null || isBlank(question.q()) || answer.isBlank()) {
                continue;
            }
            questions.add(new CourseQuestionBankQuestionSeed(
                    "single",
                    question.q().trim(),
                    List.of("A. 正确", "B. 错误"),
                    answer,
                    "正确答案：" + ("A".equals(answer) ? "正确" : "错误") + "；题型：判断题",
                    "基础到综合",
                    sourceName,
                    sortOrder
            ));
            sortOrder += 10;
        }
        return sortOrder;
    }

    private List<String> normalizeOptions(List<String> sourceOptions) {
        if (sourceOptions == null || sourceOptions.isEmpty()) {
            return List.of();
        }
        List<String> options = new ArrayList<>();
        for (String option : sourceOptions) {
            if (isBlank(option) || options.size() >= LETTERS.length()) {
                continue;
            }
            options.add(LETTERS.charAt(options.size()) + ". " + option.trim());
        }
        return options;
    }

    private String normalizeAnswerLetters(String answer) {
        if (answer == null) {
            return "";
        }
        return answer
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[,，、\\s]+", "")
                .replaceAll("[^A-Z]", "");
    }

    private String normalizeJudgeAnswer(String answer) {
        String normalized = answer == null ? "" : answer.trim();
        if (List.of("对", "正确", "是", "√", "✓", "T", "TRUE").contains(normalized.toUpperCase(Locale.ROOT))) {
            return "A";
        }
        if (List.of("错", "错误", "否", "×", "X", "F", "FALSE").contains(normalized.toUpperCase(Locale.ROOT))) {
            return "B";
        }
        String letters = normalizeAnswerLetters(answer);
        return "A".equals(letters) || "B".equals(letters) ? letters : "";
    }

    private String toDelimitedAnswer(String letters) {
        if (letters.length() <= 1) {
            return letters;
        }
        List<String> keys = new ArrayList<>();
        for (int index = 0; index < letters.length(); index++) {
            keys.add(String.valueOf(letters.charAt(index)));
        }
        return String.join(",", keys);
    }

    private String answerText(List<String> options, String answer) {
        List<String> texts = new ArrayList<>();
        for (String key : answer.split(",")) {
            String normalizedKey = key.trim().toUpperCase(Locale.ROOT);
            options.stream()
                    .filter(option -> option.toUpperCase(Locale.ROOT).startsWith(normalizedKey + "."))
                    .map(option -> option.substring(option.indexOf('.') + 1).trim())
                    .findFirst()
                    .ifPresent(texts::add);
        }
        return String.join("；", texts);
    }

    private String buildExplanation(String answer, String answerText, String typeLabel) {
        String prefix = answerText.isBlank()
                ? "正确答案：" + answer
                : "正确答案：" + answer + "。" + answerText;
        return prefix + "；题型：" + typeLabel;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record MaoismQuestionBank(
            List<MaoismQuestion> s,
            List<MaoismQuestion> m,
            List<MaoismQuestion> t
    ) {
    }

    private record MaoismQuestion(
            String q,
            List<String> o,
            String a
    ) {
    }
}
