package com.cupk.academy.service;

import com.cupk.academy.repository.QuestionBankRepository;
import com.cupk.academy.repository.QuestionBankRepository.CourseQuestionBankQuestionSeed;
import com.cupk.academy.service.QuestionBankSourceResolver.SourceFile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        importVocabulary("cet4", "cet4-vocabulary.json", "CET-4 词汇");
        importVocabulary("cet6", "cet6-vocabulary.json", "CET-6 词汇");
    }

    private void importVocabulary(String setCode, String fileName, String difficultyLabel) {
        Optional<SourceFile> sourceFile = QuestionBankSourceResolver.find(fileName, log);
        if (sourceFile.isEmpty()) {
            log.warn("CET vocabulary source file {} not found", fileName);
            return;
        }

        try (Reader reader = sourceFile.get().openReader()) {
            List<CetVocabularyWord> words = objectMapper.readValue(reader, VOCABULARY_LIST);
            List<CourseQuestionBankQuestionSeed> questions = toQuestions(words, difficultyLabel, sourceFile.get().fileName());
            if (questions.isEmpty()) {
                log.warn("CET vocabulary source file {} has no usable words", sourceFile.get().location());
                return;
            }

            long existing = questionBankRepository.countCourseQuestionBankQuestions(setCode, "vocabulary");
            if (existing >= questions.size()) {
                log.info("CET vocabulary bank {} already has {} words, skip seeding", setCode, existing);
                return;
            }

            questionBankRepository.deleteCourseQuestionBankQuestions(setCode);
            questionBankRepository.batchInsertCourseQuestionBankQuestions(setCode, questions);
            log.info("Seeded {} words into CET vocabulary bank {} from {}", questions.size(), setCode, sourceFile.get().location());
        } catch (IOException ex) {
            log.warn("Failed to seed CET vocabulary bank {} from {}", setCode, sourceFile.get().location(), ex);
        }
    }

    private List<CourseQuestionBankQuestionSeed> toQuestions(
            List<CetVocabularyWord> words,
            String difficultyLabel,
            String sourceName
    ) {
        List<CourseQuestionBankQuestionSeed> questions = new ArrayList<>();
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
