package com.cupk.academy.service;

import com.cupk.academy.repository.QuestionBankRepository;
import com.cupk.academy.repository.QuestionBankRepository.CourseQuestionBankQuestionSeed;
import com.cupk.academy.service.QuestionBankSourceResolver.SourceFile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MarxismQuestionBankSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MarxismQuestionBankSeeder.class);
    private static final String SET_CODE = "marxism";
    private static final String SOURCE_FILE = "marxism.json";
    private static final TypeReference<List<MarxismQuestion>> QUESTION_LIST = new TypeReference<>() {
    };

    private final QuestionBankRepository questionBankRepository;
    private final ObjectMapper objectMapper;

    public MarxismQuestionBankSeeder(QuestionBankRepository questionBankRepository, ObjectMapper objectMapper) {
        this.questionBankRepository = questionBankRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        Optional<SourceFile> sourceFile = QuestionBankSourceResolver.find(SOURCE_FILE, log);
        if (sourceFile.isEmpty()) {
            log.warn("Marxism question bank source file not found");
            return;
        }

        try (Reader reader = sourceFile.get().openReader()) {
            List<MarxismQuestion> sourceQuestions = objectMapper.readValue(reader, QUESTION_LIST);
            List<CourseQuestionBankQuestionSeed> questions = toSeeds(sourceQuestions, sourceFile.get().fileName());
            if (questions.isEmpty()) {
                log.warn("Marxism question bank source file {} has no usable questions", sourceFile.get().location());
                return;
            }

            long existing = questionBankRepository.countCourseQuestionBankQuestions(SET_CODE, "single")
                    + questionBankRepository.countCourseQuestionBankQuestions(SET_CODE, "multiple");
            if (existing >= questions.size()) {
                log.info("Marxism question bank already has {} questions, skip seeding", existing);
                return;
            }

            questionBankRepository.deleteCourseQuestionBankQuestions(SET_CODE);
            questionBankRepository.batchInsertCourseQuestionBankQuestions(SET_CODE, questions);
            log.info("Seeded {} marxism questions from {}", questions.size(), sourceFile.get().location());
        } catch (IOException ex) {
            log.warn("Failed to seed marxism question bank from {}", sourceFile.get().location(), ex);
        }
    }

    private List<CourseQuestionBankQuestionSeed> toSeeds(List<MarxismQuestion> sourceQuestions, String sourceName) {
        List<MarxismQuestion> usableQuestions = sourceQuestions == null ? List.of() : sourceQuestions;
        return usableQuestions.stream()
                .filter(question -> question != null && !isBlank(question.q()) && !isBlank(question.answer()))
                .filter(question -> question.options() != null && !question.options().isEmpty())
                .sorted(Comparator.comparingInt(MarxismQuestion::safeNo))
                .map(question -> toSeed(question, sourceName))
                .filter(seed -> seed != null)
                .toList();
    }

    private CourseQuestionBankQuestionSeed toSeed(MarxismQuestion question, String sourceName) {
        List<String> options = normalizeOptions(question.options());
        String answer = toDelimitedAnswer(normalizeAnswerLetters(question.answer()));
        if (options.isEmpty() || answer.isBlank()) {
            return null;
        }

        String type = isMultipleQuestion(question.type(), answer) ? "multiple" : "single";
        String typeLabel = "multiple".equals(type) ? "多选题" : "单选题";
        String answerText = answerText(options, answer);
        return new CourseQuestionBankQuestionSeed(
                type,
                question.q().trim(),
                options,
                answer,
                buildExplanation(answer, answerText, typeLabel),
                "基础到综合",
                sourceName,
                Math.max(1, question.safeNo()) * 10
        );
    }

    private List<String> normalizeOptions(List<MarxismOption> sourceOptions) {
        List<String> options = new ArrayList<>();
        for (MarxismOption option : sourceOptions) {
            if (option == null || isBlank(option.k()) || isBlank(option.v())) {
                continue;
            }
            options.add(option.k().trim().toUpperCase(Locale.ROOT) + ". " + option.v().trim());
        }
        return options;
    }

    private boolean isMultipleQuestion(String sourceType, String answer) {
        if (sourceType != null && sourceType.contains("多")) {
            return true;
        }
        return answer.split(",").length > 1;
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

    private record MarxismQuestion(
            Integer no,
            String type,
            String q,
            List<MarxismOption> options,
            String answer
    ) {
        private int safeNo() {
            return no == null ? Integer.MAX_VALUE : no;
        }
    }

    private record MarxismOption(
            String k,
            String v
    ) {
    }
}
