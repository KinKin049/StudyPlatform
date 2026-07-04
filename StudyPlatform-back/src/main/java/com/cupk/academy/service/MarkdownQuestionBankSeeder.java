package com.cupk.academy.service;

import com.cupk.academy.repository.QuestionBankRepository;
import com.cupk.academy.repository.QuestionBankRepository.CourseQuestionBankQuestionSeed;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MarkdownQuestionBankSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MarkdownQuestionBankSeeder.class);
    private static final Pattern MYSQL_QUESTION = Pattern.compile(
            "^\\*\\*(\\d+)\\.\\s*(.*?)\\*\\*\\s*(.*?)(?=^\\*\\*\\d+\\.\\s|^##\\s|\\z)",
            Pattern.MULTILINE | Pattern.DOTALL
    );
    private static final Pattern MYSQL_OPTION = Pattern.compile("^([A-D])\\)\\s*(.+?)\\s*$", Pattern.MULTILINE);
    private static final Pattern PYTHON_QUESTION = Pattern.compile(
            "^(\\d+)\\.\\s*(.*?)(?=^\\d+\\.|^##\\s|\\z)",
            Pattern.MULTILINE | Pattern.DOTALL
    );
    private static final Pattern PYTHON_OPTION = Pattern.compile("^([A-D])\\s+(.+?)\\s*$", Pattern.MULTILINE);
    private static final Pattern PYTHON_ANSWER = Pattern.compile("\\*\\*\\s*正确答案\\s+([A-Z]+)\\s*\\*\\*");
    private static final Pattern NCRE_SECTION = Pattern.compile("^##\\s+(.+?)\\s*$", Pattern.MULTILINE);
    private static final Pattern NCRE_CARD = Pattern.compile("^###\\s+(\\d+)\\.\\s*(.+?)\\s*$", Pattern.MULTILINE);

    private final QuestionBankRepository questionBankRepository;

    public MarkdownQuestionBankSeeder(QuestionBankRepository questionBankRepository) {
        this.questionBankRepository = questionBankRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("database", "National-Computer-Rank-Examination-Level-2-MySQL", this::parseMysqlQuestions);
        seed("python", "python-study-note", this::parsePythonQuestions);
        seed("ncre", "jisuanjierji", this::parseNcreCards);
    }

    private void seed(String setCode, String sourceMarker, MarkdownParser parser) {
        Optional<Path> sourcePath = findSourceFile(sourceMarker);
        if (sourcePath.isEmpty()) {
            log.warn("Markdown question bank source file {} not found", sourceMarker);
            return;
        }

        try {
            List<CourseQuestionBankQuestionSeed> questions = parser.parse(sourcePath.get());
            if (questions.isEmpty()) {
                log.warn("Markdown question bank source file {} has no usable questions", sourcePath.get());
                return;
            }

            long existing = questionBankRepository.countCourseQuestionBankQuestions(setCode, "single")
                    + questionBankRepository.countCourseQuestionBankQuestions(setCode, "multiple")
                    + questionBankRepository.countCourseQuestionBankQuestions(setCode, "short");
            if (existing >= questions.size()) {
                log.info("Markdown question bank {} already has {} questions, skip seeding", setCode, existing);
                return;
            }

            questionBankRepository.deleteCourseQuestionBankQuestions(setCode);
            questionBankRepository.batchInsertCourseQuestionBankQuestions(setCode, questions);
            log.info("Seeded {} markdown questions into {} from {}", questions.size(), setCode, sourcePath.get());
        } catch (IOException ex) {
            log.warn("Failed to seed markdown question bank {} from {}", setCode, sourcePath.get(), ex);
        }
    }

    private Optional<Path> findSourceFile(String sourceMarker) {
        List<Path> directories = new ArrayList<>();
        String configuredDir = System.getenv("QUESTION_BANK_SOURCE_DIR");
        if (configuredDir != null && !configuredDir.isBlank()) {
            directories.add(Path.of(configuredDir));
        }
        directories.add(Path.of("CET46"));
        directories.add(Path.of("..", "CET46"));
        directories.add(Path.of("..", "..", "CET46"));

        String normalizedMarker = sourceMarker.toLowerCase(Locale.ROOT);
        for (Path directory : directories) {
            Path normalizedDirectory = directory.toAbsolutePath().normalize();
            if (!Files.isDirectory(normalizedDirectory)) {
                continue;
            }
            try (Stream<Path> paths = Files.list(normalizedDirectory)) {
                Optional<Path> match = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).contains(normalizedMarker))
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md"))
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

    private List<CourseQuestionBankQuestionSeed> parseMysqlQuestions(Path sourcePath) throws IOException {
        String markdown = readMarkdown(sourcePath);
        String sourceName = sourcePath.getFileName().toString();
        List<CourseQuestionBankQuestionSeed> questions = new ArrayList<>();
        Matcher matcher = MYSQL_QUESTION.matcher(markdown);
        int sortOrder = 10;
        while (matcher.find()) {
            String section = headingFor(markdown, matcher.start());
            String stem = cleanMarkdown(matcher.group(2));
            String body = matcher.group(3);
            List<OptionSeed> options = extractOptions(body, MYSQL_OPTION);
            String preOptions = textBeforeFirstOption(body, options);
            String explanation = extractExplanation(body, options);
            if (!preOptions.isBlank()) {
                stem = joinParts(stem, cleanMarkdown(preOptions));
            }

            String answer = normalizeAnswer(detectAnswer(explanation, options));
            if (options.isEmpty() || answer.isBlank()) {
                String fallbackAnswer = explanation.isBlank() ? "参考源文件解析。" : explanation;
                String shortStem = options.isEmpty() ? stem : stem + "\n\n" + optionLines(options);
                questions.add(shortQuestion(shortStem, fallbackAnswer, section, sourceName, sortOrder));
            } else {
                questions.add(choiceQuestion(stem, options, answer, explanation, section, sourceName, sortOrder));
            }
            sortOrder += 10;
        }
        return questions;
    }

    private List<CourseQuestionBankQuestionSeed> parsePythonQuestions(Path sourcePath) throws IOException {
        String markdown = readMarkdown(sourcePath);
        String sourceName = sourcePath.getFileName().toString();
        List<CourseQuestionBankQuestionSeed> questions = new ArrayList<>();
        Matcher matcher = PYTHON_QUESTION.matcher(markdown);
        int sortOrder = 10;
        while (matcher.find()) {
            String section = headingFor(markdown, matcher.start());
            String block = matcher.group(2);
            List<OptionSeed> options = extractOptions(block, PYTHON_OPTION);
            Matcher answerMatcher = PYTHON_ANSWER.matcher(block);
            String answer = answerMatcher.find() ? normalizeAnswer(answerMatcher.group(1)) : "";
            if (options.isEmpty() || answer.isBlank()) {
                continue;
            }

            String stem = cleanMarkdown(textBeforeFirstOption(block, options));
            String explanation = answerMatcher.find(0)
                    ? cleanMarkdown(block.substring(answerMatcher.end()))
                    : "";
            questions.add(choiceQuestion(stem, options, answer, explanation, section, sourceName, sortOrder));
            sortOrder += 10;
        }
        return questions;
    }

    private List<CourseQuestionBankQuestionSeed> parseNcreCards(Path sourcePath) throws IOException {
        String markdown = readMarkdown(sourcePath);
        String sourceName = sourcePath.getFileName().toString();
        List<CourseQuestionBankQuestionSeed> questions = new ArrayList<>();
        Matcher matcher = NCRE_CARD.matcher(markdown);
        int sortOrder = 10;
        while (matcher.find()) {
            int cardEnd = nextCardOrSectionStart(markdown, matcher.end());
            String category = headingFor(markdown, matcher.start());
            String title = cleanMarkdown(matcher.group(2));
            String answer = cleanMarkdown(markdown.substring(matcher.end(), cardEnd));
            if (title.isBlank() || answer.isBlank()) {
                continue;
            }
            questions.add(shortQuestion(category + "：" + title, answer, category, sourceName, sortOrder));
            sortOrder += 10;
        }
        return questions;
    }

    private List<OptionSeed> extractOptions(String text, Pattern optionPattern) {
        List<OptionSeed> options = new ArrayList<>();
        Matcher matcher = optionPattern.matcher(text);
        while (matcher.find()) {
            String key = matcher.group(1).trim().toUpperCase(Locale.ROOT);
            String value = cleanMarkdown(matcher.group(2));
            if (value.isBlank()) {
                continue;
            }
            options.add(new OptionSeed(key, value, matcher.start(), matcher.end()));
        }
        return options;
    }

    private String detectAnswer(String explanation, List<OptionSeed> options) {
        String text = stripControlChars(explanation);
        List<Pattern> patterns = List.of(
                Pattern.compile("【答案】\\s*([A-D])", Pattern.CASE_INSENSITIVE),
                Pattern.compile("答案\\s*[：:]\\s*([A-D])", Pattern.CASE_INSENSITIVE),
                Pattern.compile("正确答案\\s*(?:是|为|：|:)?\\s*([A-D])", Pattern.CASE_INSENSITIVE),
                Pattern.compile("答案\\s*(?:是|为)\\s*([A-D])", Pattern.CASE_INSENSITIVE),
                Pattern.compile("选择\\s*([A-D])\\)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("选\\s*([A-D])\\)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("([A-D])\\)\\s*正确", Pattern.CASE_INSENSITIVE)
        );
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return inferAnswerFromOptionText(text, options);
    }

    private String inferAnswerFromOptionText(String explanation, List<OptionSeed> options) {
        String normalizedExplanation = compactForMatch(explanation);
        List<String> matches = new ArrayList<>();
        for (OptionSeed option : options) {
            String normalizedOption = compactForMatch(option.value());
            if (normalizedOption.length() >= 2 && normalizedExplanation.contains(normalizedOption)) {
                matches.add(option.key());
            }
        }
        return matches.size() == 1 ? matches.get(0) : "";
    }

    private CourseQuestionBankQuestionSeed choiceQuestion(
            String stem,
            List<OptionSeed> options,
            String answer,
            String explanation,
            String section,
            String sourceName,
            int sortOrder
    ) {
        String normalizedAnswer = toDelimitedAnswer(answer);
        String answerText = answerText(options, normalizedAnswer);
        String fullExplanation = buildExplanation(normalizedAnswer, answerText, "单选题", section, explanation);
        return new CourseQuestionBankQuestionSeed(
                "single",
                stem.trim(),
                options.stream().map(OptionSeed::display).toList(),
                normalizedAnswer,
                fullExplanation,
                "基础到综合",
                sourceName,
                sortOrder
        );
    }

    private CourseQuestionBankQuestionSeed shortQuestion(
            String stem,
            String answer,
            String section,
            String sourceName,
            int sortOrder
    ) {
        String explanation = section.isBlank() ? "来自 Markdown 源文件。" : "章节：" + section;
        return new CourseQuestionBankQuestionSeed(
                "short",
                stem.trim(),
                List.of(),
                answer.trim(),
                explanation,
                "错题复习",
                sourceName,
                sortOrder
        );
    }

    private String buildExplanation(String answer, String answerText, String typeLabel, String section, String explanation) {
        List<String> parts = new ArrayList<>();
        parts.add(answerText.isBlank() ? "正确答案：" + answer : "正确答案：" + answer + "。" + answerText);
        if (!section.isBlank()) {
            parts.add("章节：" + section);
        }
        parts.add("题型：" + typeLabel);
        if (!explanation.isBlank()) {
            parts.add(explanation);
        }
        return String.join("；", parts);
    }

    private String answerText(List<OptionSeed> options, String answer) {
        List<String> texts = new ArrayList<>();
        for (String key : answer.split(",")) {
            String normalizedKey = key.trim().toUpperCase(Locale.ROOT);
            options.stream()
                    .filter(option -> option.key().equals(normalizedKey))
                    .map(OptionSeed::value)
                    .findFirst()
                    .ifPresent(texts::add);
        }
        return String.join("；", texts);
    }

    private String readMarkdown(Path sourcePath) throws IOException {
        return stripControlChars(Files.readString(sourcePath, StandardCharsets.UTF_8))
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }

    private String textBeforeFirstOption(String text, List<OptionSeed> options) {
        if (options.isEmpty()) {
            int marker = answerMarkerIndex(text);
            return marker >= 0 ? text.substring(0, marker) : text;
        }
        return text.substring(0, options.getFirst().start());
    }

    private String extractExplanation(String text, List<OptionSeed> options) {
        int marker = answerMarkerIndex(text);
        if (marker >= 0) {
            return cleanMarkdown(text.substring(marker).replaceFirst("^\\*\\*参考答案及解析\\*\\*", ""));
        }
        if (!options.isEmpty()) {
            return cleanMarkdown(text.substring(options.getLast().end()));
        }
        return "";
    }

    private int answerMarkerIndex(String text) {
        int marker = text.indexOf("**参考答案及解析**");
        if (marker >= 0) {
            return marker;
        }
        return text.indexOf("参考答案及解析");
    }

    private String headingFor(String markdown, int index) {
        Matcher matcher = NCRE_SECTION.matcher(markdown);
        String heading = "";
        while (matcher.find()) {
            if (matcher.start() > index) {
                break;
            }
            String candidate = cleanMarkdown(matcher.group(1));
            if (!candidate.startsWith("考前速记清单") && !candidate.equals("---")) {
                heading = candidate;
            }
        }
        return heading;
    }

    private int nextCardOrSectionStart(String markdown, int fromIndex) {
        Matcher cardMatcher = NCRE_CARD.matcher(markdown);
        int next = markdown.length();
        if (cardMatcher.find(fromIndex)) {
            next = Math.min(next, cardMatcher.start());
        }
        Matcher sectionMatcher = NCRE_SECTION.matcher(markdown);
        if (sectionMatcher.find(fromIndex)) {
            next = Math.min(next, sectionMatcher.start());
        }
        return next;
    }

    private String optionLines(List<OptionSeed> options) {
        return String.join("\n", options.stream().map(OptionSeed::display).toList());
    }

    private String joinParts(String first, String second) {
        if (first.isBlank()) {
            return second;
        }
        if (second.isBlank()) {
            return first;
        }
        return first + "\n" + second;
    }

    private String normalizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[,，、\\s]+", "")
                .replaceAll("[^A-Z]", "");
    }

    private String toDelimitedAnswer(String answer) {
        if (answer.length() <= 1) {
            return answer;
        }
        List<String> keys = new ArrayList<>();
        for (int index = 0; index < answer.length(); index++) {
            keys.add(String.valueOf(answer.charAt(index)));
        }
        return String.join(",", keys);
    }

    private String cleanMarkdown(String value) {
        if (value == null) {
            return "";
        }
        return stripControlChars(value)
                .replaceAll("(?m)^```\\w*\\s*$", "")
                .replaceAll("(?m)^```\\s*$", "")
                .replace("**", "")
                .replace("【解析】", "")
                .replace("（解析）", "")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String stripControlChars(String value) {
        return value == null
                ? ""
                : value.replaceAll("[\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF]", "");
    }

    private String compactForMatch(String value) {
        return cleanMarkdown(value).replaceAll("[\\s\\p{Punct}，。！？；：“”‘’（）《》、·]+", "");
    }

    @FunctionalInterface
    private interface MarkdownParser {
        List<CourseQuestionBankQuestionSeed> parse(Path sourcePath) throws IOException;
    }

    private record OptionSeed(String key, String value, int start, int end) {
        private String display() {
            return key + ". " + value;
        }
    }
}
