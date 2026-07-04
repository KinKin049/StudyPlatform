package com.cupk.academy.service;

import com.cupk.academy.repository.QuestionBankRepository;
import com.cupk.academy.repository.QuestionBankRepository.CourseQuestionBankQuestionSeed;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CetVocabularySeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(CetVocabularySeeder.class);
    private static final TypeReference<List<CetVocabularyWord>> VOCABULARY_LIST = new TypeReference<>() {
    };

    private final QuestionBankRepository questionBankRepository;
    private final ObjectMapper objectMapper;

    public CetVocabularySeeder(QuestionBankRepository questionBankRepository, ObjectMapper objectMapper) {
        this.questionBankRepository = questionBankRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        importVocabulary("cet4", "CET4", "CET-4 词汇");
        importVocabulary("cet6", "CET6", "CET-6 词汇");
    }

    private void importVocabulary(String setCode, String fileMarker, String difficultyLabel) {
        Optional<Path> sourcePath = findVocabularyFile(fileMarker);
        if (sourcePath.isEmpty()) {
            log.warn("CET vocabulary source file not found for marker {}", fileMarker);
            return;
        }

        try (Reader reader = Files.newBufferedReader(sourcePath.get(), StandardCharsets.UTF_8)) {
            List<CetVocabularyWord> words = objectMapper.readValue(reader, VOCABULARY_LIST);
            List<CourseQuestionBankQuestionSeed> questions = toQuestions(words, difficultyLabel, sourcePath.get());
            if (questions.isEmpty()) {
                log.warn("CET vocabulary source file {} has no usable words", sourcePath.get());
                return;
            }

            long existing = questionBankRepository.countCourseQuestionBankQuestions(setCode, "vocabulary");
            if (existing >= questions.size()) {
                log.info("CET vocabulary bank {} already has {} words, skip seeding", setCode, existing);
                return;
            }

            questionBankRepository.deleteCourseQuestionBankQuestions(setCode);
            questionBankRepository.batchInsertCourseQuestionBankQuestions(setCode, questions);
            log.info("Seeded {} words into CET vocabulary bank {} from {}", questions.size(), setCode, sourcePath.get());
        } catch (IOException ex) {
            log.warn("Failed to seed CET vocabulary bank {} from {}", setCode, sourcePath.get(), ex);
        }
    }

    private Optional<Path> findVocabularyFile(String marker) {
        List<Path> directories = new ArrayList<>();
        String configuredDir = System.getenv("CET46_DIR");
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
                        .filter(path -> {
                            String fileName = path.getFileName().toString();
                            return fileName.toLowerCase().endsWith(".json") && fileName.contains(marker);
                        })
                        .findFirst();
                if (match.isPresent()) {
                    return match;
                }
            } catch (IOException ex) {
                log.warn("Failed to scan CET vocabulary directory {}", normalizedDirectory, ex);
            }
        }
        return Optional.empty();
    }

    private List<CourseQuestionBankQuestionSeed> toQuestions(
            List<CetVocabularyWord> words,
            String difficultyLabel,
            Path sourcePath
    ) {
        List<CourseQuestionBankQuestionSeed> questions = new ArrayList<>();
        String sourceName = sourcePath.getFileName().toString();
        int sortOrder = 10;
        for (CetVocabularyWord word : words) {
            if (word == null || word.word() == null || word.word().isBlank()) {
                continue;
            }
            String answer = formatTranslations(word.translations());
            if (answer.isBlank()) {
                continue;
            }
            String normalizedWord = word.word().trim();
            questions.add(new CourseQuestionBankQuestionSeed(
                    "vocabulary",
                    normalizedWord,
                    List.of(),
                    answer,
                    "词义：" + answer,
                    difficultyLabel,
                    sourceName,
                    sortOrder
            ));
            sortOrder += 10;
        }
        return questions;
    }

    private String formatTranslations(List<CetVocabularyTranslation> translations) {
        if (translations == null || translations.isEmpty()) {
            return "";
        }
        return translations.stream()
                .map(this::formatTranslation)
                .filter(value -> !value.isBlank())
                .reduce((left, right) -> left + "；" + right)
                .orElse("");
    }

    private String formatTranslation(CetVocabularyTranslation translation) {
        if (translation == null || translation.translation() == null || translation.translation().isBlank()) {
            return "";
        }
        String text = translation.translation().trim();
        if (translation.type() == null || translation.type().isBlank()) {
            return text;
        }
        return translation.type().trim() + ". " + text;
    }

    private record CetVocabularyWord(
            String word,
            List<CetVocabularyTranslation> translations
    ) {
    }

    private record CetVocabularyTranslation(
            String translation,
            String type
    ) {
    }
}
