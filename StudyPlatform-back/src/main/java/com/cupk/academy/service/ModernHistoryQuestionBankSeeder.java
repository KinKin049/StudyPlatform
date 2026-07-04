package com.cupk.academy.service;

import com.cupk.academy.repository.QuestionBankRepository;
import com.cupk.academy.repository.QuestionBankRepository.CourseQuestionBankQuestionSeed;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ModernHistoryQuestionBankSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ModernHistoryQuestionBankSeeder.class);
    private static final String SET_CODE = "modern-history";
    private static final String SOURCE_MARKER = "zhongguojindaishigangyao";
    private static final TypeReference<List<ModernHistoryQuestion>> QUESTION_LIST = new TypeReference<>() {
    };

    private final QuestionBankRepository questionBankRepository;
    private final ObjectMapper objectMapper;

    public ModernHistoryQuestionBankSeeder(QuestionBankRepository questionBankRepository, ObjectMapper objectMapper) {
        this.questionBankRepository = questionBankRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        Optional<Path> sourcePath = findSourceFile();
        if (sourcePath.isEmpty()) {
            log.warn("Modern history question bank source file not found");
            return;
        }

        try {
            List<ModernHistoryQuestion> sourceQuestions = objectMapper.readValue(
                    extractQuestionJson(sourcePath.get()),
                    QUESTION_LIST
            );
            List<CourseQuestionBankQuestionSeed> questions = toSeeds(sourceQuestions, sourcePath.get());
            if (questions.isEmpty()) {
                log.warn("Modern history question bank source file {} has no usable questions", sourcePath.get());
                return;
            }

            long existing = questionBankRepository.countCourseQuestionBankQuestions(SET_CODE, "single");
            if (existing >= questions.size()) {
                log.info("Modern history question bank already has {} questions, skip seeding", existing);
                return;
            }

            questionBankRepository.deleteCourseQuestionBankQuestions(SET_CODE);
            questionBankRepository.batchInsertCourseQuestionBankQuestions(SET_CODE, questions);
            log.info("Seeded {} modern history questions from {}", questions.size(), sourcePath.get());
        } catch (IOException ex) {
            log.warn("Failed to seed modern history question bank from {}", sourcePath.get(), ex);
        }
    }

    private Optional<Path> findSourceFile() {
        List<Path> directories = new ArrayList<>();
        String configuredDir = System.getenv("QUESTION_BANK_SOURCE_DIR");
        if (configuredDir != null && !configuredDir.isBlank()) {
            directories.add(Path.of(configuredDir));
        }
        directories.add(Path.of("CET46"));
        directories.add(Path.of("..", "CET46"));
        directories.add(Path.of("..", "..", "CET46"));

        for (Path directory : directories) {
            Path normalizedDirectory = directory.toAbsolutePath().normalize();
            if (!Files.isDirectory(normalizedDirectory)) {
                continue;
            }
            try (Stream<Path> paths = Files.list(normalizedDirectory)) {
                Optional<Path> match = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase().contains(SOURCE_MARKER))
                        .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".html"))
                        .findFirst();
                if (match.isPresent()) {
                    return match;
                }
            } catch (IOException ex) {
                log.warn("Failed to scan question bank source directory {}", normalizedDirectory, ex);
            }
        }
        return Optional.empty();
    }

    private String extractQuestionJson(Path sourcePath) throws IOException {
        String html = Files.readString(sourcePath, StandardCharsets.UTF_8);
        int declarationIndex = html.indexOf("const ALL");
        if (declarationIndex < 0) {
            throw new IOException("const ALL declaration not found");
        }
        int arrayStart = html.indexOf('[', declarationIndex);
        if (arrayStart < 0) {
            throw new IOException("question array start not found");
        }
        int arrayEnd = html.indexOf(";\nlet qlist", arrayStart);
        if (arrayEnd < 0) {
            arrayEnd = html.indexOf(";\r\nlet qlist", arrayStart);
        }
        if (arrayEnd < 0) {
            throw new IOException("question array end not found");
        }
        return html.substring(arrayStart, arrayEnd);
    }

    private List<CourseQuestionBankQuestionSeed> toSeeds(List<ModernHistoryQuestion> sourceQuestions, Path sourcePath) {
        String sourceName = sourcePath.getFileName().toString();
        return sourceQuestions.stream()
                .filter(question -> question.question() != null && !question.question().isBlank())
                .filter(question -> question.answer() != null && !question.answer().isBlank())
                .filter(question -> question.options() != null && !question.options().isEmpty())
                .sorted(Comparator.comparingInt(ModernHistoryQuestion::safeId))
                .map(question -> toSeed(question, sourceName))
                .toList();
    }

    private CourseQuestionBankQuestionSeed toSeed(ModernHistoryQuestion question, String sourceName) {
        List<String> options = question.options().stream()
                .filter(option -> option.letter() != null && option.text() != null)
                .map(option -> option.letter().trim().toUpperCase() + ". " + option.text().trim())
                .toList();
        String answer = question.answer().trim().toUpperCase();
        String answerText = question.options().stream()
                .filter(option -> answer.equalsIgnoreCase(option.letter()))
                .map(ModernHistoryOption::text)
                .findFirst()
                .orElse("");
        String explanation = answerText.isBlank()
                ? "正确答案：" + answer
                : "正确答案：" + answer + "。" + answerText;
        return new CourseQuestionBankQuestionSeed(
                "single",
                question.question().trim(),
                options,
                answer,
                explanation,
                "基础到综合",
                sourceName,
                Math.max(1, question.safeId()) * 10
        );
    }

    private record ModernHistoryQuestion(
            String question,
            String answer,
            List<ModernHistoryOption> options,
            Integer id
    ) {
        private int safeId() {
            return id == null ? Integer.MAX_VALUE : id;
        }
    }

    private record ModernHistoryOption(
            String letter,
            String text
    ) {
    }
}
