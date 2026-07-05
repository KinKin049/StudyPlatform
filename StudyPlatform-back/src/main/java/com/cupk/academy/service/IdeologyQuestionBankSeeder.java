package com.cupk.academy.service;

import com.cupk.academy.repository.QuestionBankRepository;
import com.cupk.academy.repository.QuestionBankRepository.CourseQuestionBankQuestionSeed;
import com.cupk.academy.service.QuestionBankSourceResolver.SourceFile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class IdeologyQuestionBankSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(IdeologyQuestionBankSeeder.class);
    private static final String SET_CODE = "ideology";
    private static final String SOURCE_FILE = "ideology-law.html";
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Pattern OPTION_PREFIX = Pattern.compile("^\\s*([A-Z])\\s*[.、．。]\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final TypeReference<List<IdeologyQuestion>> QUESTION_LIST = new TypeReference<>() {
    };

    private final QuestionBankRepository questionBankRepository;
    private final ObjectMapper objectMapper;

    public IdeologyQuestionBankSeeder(QuestionBankRepository questionBankRepository, ObjectMapper objectMapper) {
        this.questionBankRepository = questionBankRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        Optional<SourceFile> sourceFile = QuestionBankSourceResolver.find(SOURCE_FILE, log);
        if (sourceFile.isEmpty()) {
            log.warn("Ideology question bank source file not found");
            return;
        }

        try {
            List<IdeologyQuestion> sourceQuestions = objectMapper.readValue(
                    extractQuestionJson(sourceFile.get()),
                    QUESTION_LIST
            );
            List<CourseQuestionBankQuestionSeed> questions = toSeeds(sourceQuestions, sourceFile.get().fileName());
            if (questions.isEmpty()) {
                log.warn("Ideology question bank source file {} has no usable questions", sourceFile.get().location());
                return;
            }

            long existing = questionBankRepository.countCourseQuestionBankQuestions(SET_CODE, "single")
                    + questionBankRepository.countCourseQuestionBankQuestions(SET_CODE, "multiple");
            if (existing >= questions.size()) {
                log.info("Ideology question bank already has {} questions, skip seeding", existing);
                return;
            }

            questionBankRepository.deleteCourseQuestionBankQuestions(SET_CODE);
            questionBankRepository.batchInsertCourseQuestionBankQuestions(SET_CODE, questions);
            log.info("Seeded {} ideology questions from {}", questions.size(), sourceFile.get().location());
        } catch (IOException ex) {
            log.warn("Failed to seed ideology question bank from {}", sourceFile.get().location(), ex);
        }
    }

    private String extractQuestionJson(SourceFile sourceFile) throws IOException {
        String html = sourceFile.readString();
        int declarationIndex = html.indexOf("const QUIZ_DATA");
        if (declarationIndex < 0) {
            throw new IOException("const QUIZ_DATA declaration not found");
        }
        int arrayStart = html.indexOf('[', declarationIndex);
        if (arrayStart < 0) {
            throw new IOException("question array start not found");
        }
        int arrayEnd = html.indexOf("];</script>", arrayStart);
        if (arrayEnd >= 0) {
            return html.substring(arrayStart, arrayEnd + 1);
        }

        int scriptEnd = html.indexOf("</script>", arrayStart);
        if (scriptEnd < 0) {
            throw new IOException("script end not found");
        }
        arrayEnd = html.lastIndexOf("];", scriptEnd);
        if (arrayEnd < arrayStart) {
            throw new IOException("question array end not found");
        }
        return html.substring(arrayStart, arrayEnd + 1);
    }

    private List<CourseQuestionBankQuestionSeed> toSeeds(List<IdeologyQuestion> sourceQuestions, String sourceName) {
        List<CourseQuestionBankQuestionSeed> questions = new ArrayList<>();
        int sortOrder = 10;
        for (IdeologyQuestion question : sourceQuestions) {
            CourseQuestionBankQuestionSeed seed = toSeed(question, sourceName, sortOrder);
            if (seed == null) {
                continue;
            }
            questions.add(seed);
            sortOrder += 10;
        }
        return questions;
    }

    private CourseQuestionBankQuestionSeed toSeed(IdeologyQuestion question, String sourceName, int sortOrder) {
        if (question == null || isBlank(question.text()) || isBlank(question.answer())) {
            return null;
        }
        String sourceType = normalizeSourceType(question.type());
        if (sourceType.isBlank()) {
            return null;
        }

        List<String> options = "judge".equals(sourceType)
                ? List.of("A. 正确", "B. 错误")
                : normalizeOptions(question.options());
        if (options.isEmpty()) {
            return null;
        }

        String answer = "judge".equals(sourceType)
                ? normalizeJudgeAnswer(question.answer())
                : toDelimitedAnswer(normalizeAnswerLetters(question.answer()));
        if (answer.isBlank()) {
            return null;
        }

        String questionType = "multi".equals(sourceType) ? "multiple" : "single";
        String answerText = answerText(options, answer);
        String displayAnswer = "judge".equals(sourceType) && !answerText.isBlank() ? answerText : answer;
        return new CourseQuestionBankQuestionSeed(
                questionType,
                question.text().trim(),
                options,
                answer,
                buildExplanation(displayAnswer, answerText, question.chapter(), sourceType),
                "基础到综合",
                sourceName,
                sortOrder
        );
    }

    private List<String> normalizeOptions(List<String> sourceOptions) {
        if (sourceOptions == null || sourceOptions.isEmpty()) {
            return List.of();
        }
        List<String> options = new ArrayList<>();
        for (String option : sourceOptions) {
            if (isBlank(option)) {
                continue;
            }
            int index = options.size();
            String fallbackLetter = index < LETTERS.length() ? String.valueOf(LETTERS.charAt(index)) : "";
            Matcher matcher = OPTION_PREFIX.matcher(option);
            if (matcher.matches()) {
                options.add(matcher.group(1).toUpperCase(Locale.ROOT) + ". " + matcher.group(2).trim());
            } else if (!fallbackLetter.isBlank()) {
                options.add(fallbackLetter + ". " + option.trim());
            }
        }
        return options;
    }

    private String normalizeSourceType(String type) {
        if (type == null) {
            return "";
        }
        return switch (type.trim().toLowerCase(Locale.ROOT)) {
            case "single" -> "single";
            case "multi", "multiple" -> "multi";
            case "judge" -> "judge";
            default -> "";
        };
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

    private String buildExplanation(String displayAnswer, String answerText, String chapter, String sourceType) {
        List<String> parts = new ArrayList<>();
        parts.add(answerText.isBlank() || answerText.equals(displayAnswer)
                ? "正确答案：" + displayAnswer
                : "正确答案：" + displayAnswer + "。" + answerText);
        if (!isBlank(chapter)) {
            parts.add("章节：" + chapter.trim());
        }
        parts.add("题型：" + typeLabel(sourceType));
        return String.join("；", parts);
    }

    private String typeLabel(String sourceType) {
        return switch (sourceType) {
            case "multi" -> "多选题";
            case "judge" -> "判断题";
            default -> "单选题";
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record IdeologyQuestion(
            String chapter,
            String type,
            Integer num,
            String text,
            List<String> options,
            String answer
    ) {
    }
}
